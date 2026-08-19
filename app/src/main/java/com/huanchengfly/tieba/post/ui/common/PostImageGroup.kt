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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.LongPicChip
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage

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
        PostImageGrid(pics = pics, photoViewDataProvider = photoViewDataProvider)
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
