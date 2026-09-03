package com.huanchengfly.tieba.post.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.LongPicChip
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.PlaceholderRetry
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.shimmer
import com.google.accompanist.placeholder.PlaceholderHighlight
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

/** 当前渲染图片组所在的 LazyColumn, 供展开/收起时保持滚动位置. */
internal val LocalLazyColumnState = compositionLocalOf<LazyListState?> { null }

/**
 * 长图展开后, 供上下楼导航把「每张展开图 / 收起按钮」当作中间站点.
 *
 * @param postId 所属楼层 id
 * @param itemTopY 该楼层 item 顶部在窗口中的 Y, 用于换算成 item 内偏移
 * @param report 上报升序的 item 内偏移列表(每张展开图 + 最后的收起按钮)
 */
@Immutable
class LongImageNavContext(
    val postId: Long,
    val itemTopY: () -> Int,
    val report: (List<Int>) -> Unit,
    val topInsetPx: Int,
)

internal val LocalLongImageNavContext = compositionLocalOf<LongImageNavContext?> { null }

private val GridSpacing = 4.dp

/** 单张超长图的展示高度上限, 超出部分点击查看原图. */
private val LongImageMaxHeight = 360.dp

/** 判断是否为竖长图(宽高比小于 0.5). */
private fun PicContentRender.isLongImage(): Boolean {
    val size = dimensions ?: return false
    return size.height >= size.width * 2f
}

/**
 * 渲染一帖的内容渲染列表, 连续的图片合并为网格, 其余渲染保持原样.
 *
 * @param photoViewDataProvider 可选, 用于为缺少图集数据的图片构建 [PhotoViewData]
 *   (如 SubPostsPage 中渲染楼中楼图片时), 默认使用图片自带的数据.
 */
@Composable
fun PostContentRenders(
    contentRenders: List<PbContentRender>,
    photoViewDataProvider: ((List<PicContentRender>, Int) -> PhotoViewData?)? = null,
) {
    // 统计这楼里有几组连续图片, 用于收起时决定回楼顶还是回本组位置
    var groupCount = 0
    var scan = 0
    while (scan < contentRenders.size) {
        if (contentRenders[scan] is PicContentRender) {
            groupCount++
            while (scan < contentRenders.size && contentRenders[scan] is PicContentRender) scan++
        } else {
            scan++
        }
    }
    val isSoleGroup = groupCount == 1
    var index = 0
    while (index < contentRenders.size) {
        val render = contentRenders[index]
        if (render is PicContentRender) {
            var end = index + 1
            while (end < contentRenders.size && contentRenders[end] is PicContentRender) end++
            PostImageGroup(
                pics = contentRenders.subList(index, end).filterIsInstance<PicContentRender>(),
                photoViewDataProvider = photoViewDataProvider,
                isSoleGroup = isSoleGroup,
            )
            index = end
        } else {
            render.Render()
            index++
        }
    }
}

/**
 * 渲染一组连续图片: 单图保持原比例, 多图压缩为缩略网格.
 */
@Composable
fun PostImageGroup(
    pics: List<PicContentRender>,
    photoViewDataProvider: ((List<PicContentRender>, Int) -> PhotoViewData?)? = null,
    isSoleGroup: Boolean = true,
) {
    val hasLong = pics.any { it.isLongImage() }
    if (pics.size == 1 && !hasLong) {
        SinglePostImage(pic = pics[0], photoViewDataProvider = photoViewDataProvider)
    } else {
        val scope = rememberCoroutineScope()
        val listState = LocalLazyColumnState.current
        var expanded by rememberSaveable(pics) { mutableStateOf(false) }
        val navContext = LocalLongImageNavContext.current
        // step -> item 内偏移; 最后一个 step 是收起按钮.
        // 只能在 onGloballyPositioned 回调「当下」把坐标换算成标量存起来:
        // 回调给的 LayoutCoordinates 是修饰符节点的 coordinator 实例, 会被对象池回收复用,
        // 把它存下来到后面的帧再读, 拿到的是别的节点的坐标(实测偏移退化成 0/错值)
        val waypointOffsets = remember(pics) { mutableStateMapOf<Int, Int>() }
        fun recordWaypoint(step: Int, y: Float) {
            val context = navContext ?: return
            waypointOffsets[step] = (y - context.itemTopY()).toInt().coerceAtLeast(0)
            scope.launch {
                withFrameNanos { }
                val ordered = (0 until pics.size).map { waypointOffsets[it] } +
                        waypointOffsets[pics.size]
                if (ordered.all { it != null }) {
                    context.report(ordered.map { it!! })
                }
            }
        }
        Column {
            if (hasLong && expanded) {
                // 展开态: 长图全宽封顶显示, 非长图保持各自比例, 恢复为展开前的纵向排列
                Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
                    pics.forEachIndexed { index, pic ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { recordWaypoint(index, it.positionInWindow().y) },
                        ) {
                            if (pic.isLongImage()) {
                                ExpandedLongImage(pic = pic, photoViewDataProvider = photoViewDataProvider)
                            } else {
                                pic.Render()
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { recordWaypoint(pics.size, it.positionInWindow().y) },
                ) {
                    PostImageToggleButton(
                        expand = false,
                        onClick = {
                            expanded = false
                            // 收起后清除站点, 上下楼导航不再按图逐站
                            navContext?.report(emptyList())
                            // 收起后跳回所在帖项并让出置顶排序栏, 避免用户名被裁
                            // 多组图时回本组位置, 单组图/评论楼回楼顶
                            listState?.let {
                                val topInset = navContext?.topInsetPx ?: 0
                                val targetOffset =
                                        (if (isSoleGroup) 0 else (waypointOffsets[0] ?: 0)) - topInset
                                scope.launch {
                                    it.animateScrollToItem(
                                        it.firstVisibleItemIndex,
                                        scrollOffset = targetOffset,
                                    )
                                }
                            }
                        },
                    )
                }
            } else {
                if (pics.size == 1) {
                    SinglePostImage(pic = pics[0], photoViewDataProvider = photoViewDataProvider)
                } else {
                    PostImageGrid(pics = pics, photoViewDataProvider = photoViewDataProvider)
                }
                if (hasLong) {
                    PostImageToggleButton(
                        expand = true,
                        onClick = { expanded = true },
                    )
                }
            }
        }
    }
}

/**
 * 展开态下的长图: 按真实宽高比全宽显示, 不封顶高度, 随页面滚动查看完整内容.
 */
@Composable
private fun ExpandedLongImage(
    pic: PicContentRender,
    photoViewDataProvider: ((List<PicContentRender>, Int) -> PhotoViewData?)? = null,
) {
    val context = LocalContext.current
    // 用图片加载后的真实宽高比撑开盒子, 避免 bsize 与原图比例不符导致 Fit 后左右留白
    var ratio by remember(pic) {
        mutableStateOf(
            pic.dimensions?.let {
                if (it.width > 0 && it.height > 0) it.width.toFloat() / it.height else null
            } ?: 0.5f,
        )
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(pic.originUrl.ifEmpty { pic.picUrl })
            .build(),
        contentScale = ContentScale.Fit,
    )
    val painterState by painter.state.collectAsState()
    LaunchedEffect(painterState) {
        if (painterState is AsyncImagePainter.State.Success) {
            val size = painter.intrinsicSize
            if (size.width > 0f && size.height > 0f) {
                ratio = size.width / size.height
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentAlignment = Alignment.TopEnd,
    ) {
        when (painterState) {
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .pointerInput(pic) {
                            detectTapGestures {
                                val photos = photoViewDataProvider?.invoke(listOf(pic), 0) ?: pic.photoViewData
                                    ?: return@detectTapGestures
                                PhotoViewActivity.launch(context, photos)
                            }
                        },
                    contentScale = ContentScale.Fit,
                )
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LongImageMaxHeight),
                ) {
                    PlaceholderRetry(onRetry = painter::restart)
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LongImageMaxHeight)
                        .placeholder(
                            visible = true,
                            shape = MaterialTheme.shapes.small,
                            highlight = PlaceholderHighlight.shimmer(),
                        ),
                )
            }
        }
    }
}

@Composable
private fun PostImageToggleButton(
    expand: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = if (expand) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(
                id = if (expand) R.string.btn_expand_long_image else R.string.btn_collapse_long_image,
            ),
        )
    }
}

@Composable
private fun SinglePostImage(
    pic: PicContentRender,
    photoViewDataProvider: ((List<PicContentRender>, Int) -> PhotoViewData?)? = null,
) {
    if (pic.isLongImage()) {
        // 超长图: 全宽 + 高度封顶, 点击查看原图
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LongImageMaxHeight)
                .clip(MaterialTheme.shapes.small)
        ) {
            NetworkImage(
                modifier = Modifier.matchParentSize(),
                imageUrl = pic.picUrl,
                contentScale = ContentScale.Crop,
                photoViewDataProvider = {
                    photoViewDataProvider?.invoke(listOf(pic), 0) ?: pic.photoViewData
                },
            )
            LongPicChip(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    } else {
        pic.Render()
    }
}

@Composable
private fun PostImageGrid(
    pics: List<PicContentRender>,
    photoViewDataProvider: ((List<PicContentRender>, Int) -> PhotoViewData?)? = null,
) {
    val columns = when {
        pics.size == 4 -> 2
        pics.size >= 5 -> 3
        else -> pics.size
    }
    Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
        pics.chunked(columns).forEach { rowPics ->
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
                rowPics.forEach { pic ->
                    PostImageGridCell(
                        pic = pic,
                        photoViewData = photoViewDataProvider?.invoke(pics, pics.indexOf(pic)) ?: pic.photoViewData,
                    )
                }
                // 最后一行不足列数时用空白占位, 保持格子等宽
                for (i in 0 until columns - rowPics.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.PostImageGridCell(
    pic: PicContentRender,
    photoViewData: PhotoViewData?,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small),
        contentAlignment = Alignment.TopEnd,
    ) {
        NetworkImage(
            modifier = Modifier.matchParentSize(),
            imageUrl = pic.picUrl,
            dimensions = pic.dimensions,
            contentScale = ContentScale.Crop,
            photoViewDataProvider = { photoViewData },
        )
        if (pic.isLongImage()) {
            LongPicChip(modifier = Modifier.padding(6.dp))
        }
    }
}
