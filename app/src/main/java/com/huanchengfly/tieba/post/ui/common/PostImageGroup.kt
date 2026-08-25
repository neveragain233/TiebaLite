package com.huanchengfly.tieba.post.ui.common

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.LongPicChip
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

/** 当前渲染图片组所在的 LazyColumn, 供展开/收起时保持滚动位置. */
internal val LocalLazyColumnState = compositionLocalOf<LazyListState?> { null }

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
    var index = 0
    while (index < contentRenders.size) {
        val render = contentRenders[index]
        if (render is PicContentRender) {
            var end = index + 1
            while (end < contentRenders.size && contentRenders[end] is PicContentRender) end++
            PostImageGroup(
                pics = contentRenders.subList(index, end).filterIsInstance<PicContentRender>(),
                photoViewDataProvider = photoViewDataProvider,
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
) {
    if (pics.size == 1) {
        SinglePostImage(pic = pics[0], photoViewDataProvider = photoViewDataProvider)
    } else {
        val scope = rememberCoroutineScope()
        val listState = LocalLazyColumnState.current
        val hasLong = pics.any { it.isLongImage() }
        var expanded by rememberSaveable(pics) { mutableStateOf(false) }
        Column {
            if (hasLong && expanded) {
                // 展开态: 长图全宽封顶显示, 非长图保持各自比例, 恢复为展开前的纵向排列
                Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
                    pics.forEachIndexed { index, pic ->
                        if (pic.isLongImage()) {
                            ExpandedLongImage(pic = pic, photoViewDataProvider = photoViewDataProvider)
                        } else {
                            pic.Render()
                        }
                    }
                }
                PostImageToggleButton(
                    expand = false,
                    onClick = {
                        expanded = false
                        // 收起后跳回所在帖项, 避免被长图高度变化甩到列表末尾
                        listState?.let {
                            scope.launch {
                                it.animateScrollToItem(it.firstVisibleItemIndex)
                            }
                        }
                    },
                )
            } else {
                PostImageGrid(pics = pics, photoViewDataProvider = photoViewDataProvider)
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
            .pointerInput(pic) {
                detectTapGestures {
                    val photos = photoViewDataProvider?.invoke(listOf(pic), 0) ?: pic.photoViewData
                        ?: return@detectTapGestures
                    PhotoViewActivity.launch(context, photos)
                }
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        if (painterState is AsyncImagePainter.State.Success) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
            )
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
