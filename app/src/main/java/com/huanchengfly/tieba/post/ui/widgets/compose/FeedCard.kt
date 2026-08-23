package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.media3.exoplayer.ExoPlayer
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.aspectRatio
import com.huanchengfly.tieba.post.api.models.protos.buildRenders
import com.huanchengfly.tieba.post.api.models.protos.getPicUrl
import com.huanchengfly.tieba.post.api.models.protos.isExpired
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.MediaDisplayMode
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalVideoPreviewState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.PreviewVideoPlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.MediaUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class FeedType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

val ThreadContentType: (index: Int, item: ThreadItem) -> FeedType by unsafeLazy {
    { _, item ->
        when {
            item.isTop -> FeedType.Top
            item.video != null -> FeedType.Video
            item.medias?.size == 1 -> FeedType.SingleMedia
            (item.medias?.size ?: 0) > 1 -> FeedType.MultiMedia
            else -> FeedType.PlainText
        }
    }
}

internal val CardHorizontalSpacing = 16.dp

internal val DefaultCardPaddings = PaddingValues(horizontal = CardHorizontalSpacing)

/** The default thickness of Feed Card divider. */
internal val CardDividerThickness = 2.dp

/** The default amount of time that [FeedVideoPreview] should be considered visible. */
private const val VIDEO_MIN_VISIBLE_DURATION_MS = 500L

fun Modifier.cardBottomDivider(
    color: Color,
    horizontalSpacing: Dp = CardHorizontalSpacing,
): Modifier = this.drawWithCache {
    onDrawBehind {
        drawLine(
            color = color,
            strokeWidth = CardDividerThickness.toPx(),
            start = Offset(horizontalSpacing.toPx(), size.height),
            end = Offset(size.width - horizontalSpacing.toPx(), size.height - CardDividerThickness.toPx() / 2),
        )
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
    action: @Composable (ColumnScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = DefaultCardPaddings,
) {
    Column(
        modifier = modifier
            .block {
                onClick?.let { clickable(onClick = it) }
            }
            .block {
                if (action != null) padding(top = 16.dp) else padding(vertical = 16.dp)
            }
            .padding(contentPadding)
    ) {
        header()

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
            content = content
        )

        action?.invoke(this)
    }
}

@Composable
fun MediaSizeBadge(
    modifier: Modifier = Modifier,
    size: Int,
    backgroundColor: Color = Color.Black.copy(0.5f),
    contentColor: Color = Color.White,
) {
    Row(
        modifier = modifier
            .background(color = backgroundColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PhotoSizeSelectActual,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(text = size.toString(), fontSize = 12.sp, color = contentColor)
    }
}

fun buildThreadContent(
    title: String?,
    abstractText: String,
    tabName: String? = null,
    isGood: Boolean = false
): AnnotatedString = buildAnnotatedString {
    val colorScheme = ThemeUtil.currentColorScheme()
    val showTitle = !title.isNullOrBlank()
    val showAbstract = abstractText.isNotBlank()

    if (showTitle) {
        withStyle(
            style = SpanStyle(
                fontSize = 16.sp, // TypeScaleTokens.BodyLargeSize
                fontWeight = FontWeight.Bold
            )
        ) {
            if (isGood) {
                withStyle(style = SpanStyle(color = colorScheme.tertiary)) {
                    append(App.INSTANCE.getString(R.string.tip_good))
                }
                append(" ")
            }

            if (!tabName.isNullOrBlank()) {
                append(tabName)
                append(" | ")
            }

            append(title)
        }
    }
    if (showTitle && showAbstract) {
        append('\n')
    }
    if (showAbstract) {
        append(abstractText.emoticonString)
    }
}

@Composable
fun FeedCardPlaceholder() {
    Card(
        header = { UserHeaderPlaceholder(avatarSize = Sizes.Small) },
        content = {
            Text(
                text = "TitlePlaceholder",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
            )

            Text(
                text = "Text",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(highlight = PlaceholderHighlight.fade())
            )
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    ActionBtnPlaceholder(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
fun ForumInfoChip(
    modifier: Modifier = Modifier,
    forumName: String,
    avatarUrl: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
    ) {
        avatarUrl?.let {
            Avatar(
                data = avatarUrl,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                shape = MaterialTheme.shapes.extraSmall
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, forumName),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MediaPlaceholder(
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ProvideContentColorTextStyle(
        contentColor = MaterialTheme.colorScheme.onSurface,
        textStyle = MaterialTheme.typography.labelMedium
    ) {
        Row(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .onNotNull(onClick) {
                    clickable(onClick = it)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(16.dp), content = icon)
            text()
        }
    }
}

const val MAX_PHOTO_IN_ROW = 3

/** 列表/详情内媒体最大宽度(dp), 超过则封顶并居中. */
val MediaListMaxWidth = 360.dp

/** 紧凑档媒体最大宽度(dp). */
val MediaListMaxWidthCompact = 300.dp

/** 依据可用宽度返回媒体应占的宽度比例; 可用宽度不超过 [max] 时填满. */
fun mediaWidthFraction(maxWidth: Dp, max: Dp = MediaListMaxWidth): Float {
    if (maxWidth.value <= 0f) return 1f
    if (maxWidth.value <= max.value) return 1f
    return (max.value / maxWidth.value).coerceAtMost(1f)
}

@Composable
fun ThreadMedia(
    modifier: Modifier = Modifier,
    forumId: Long,
    forumName: String,
    threadId: Long,
    medias: List<Media> = persistentListOf(),
    videoInfo: ImmutableHolder<VideoInfo>? = null,
) {
    if (medias.isEmpty() && videoInfo == null) return

    val context = LocalContext.current
    val habitSettings = LocalHabitSettings.current
    val mediaCount = medias.size
    val isSinglePhoto = mediaCount == 1

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val mode = habitSettings.mediaDisplayMode
        val cap = if (mode == MediaDisplayMode.COMPACT) MediaListMaxWidthCompact else MediaListMaxWidth
        val frac = mediaWidthFraction(maxWidth, cap)
        if (videoInfo != null) {
            if (mode == MediaDisplayMode.HIDE) {
                MediaPlaceholder(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.OndemandVideo,
                            contentDescription = stringResource(id = R.string.desc_video)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.desc_video))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val videoAsGridCell = mode == MediaDisplayMode.COMPACT &&
                        habitSettings.compactSingleAsGridCell &&
                        !habitSettings.videoAutoplay
                if (videoAsGridCell) {
                    val gridWidth = maxWidth * frac
                    val columns = (gridWidth.value / CompactMediaGridTileTarget.value)
                        .roundToInt()
                        .coerceIn(2, 5)
                    val cellWidth = gridWidth / columns.toFloat()
                    FeedVideoPreview(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(cellWidth)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small),
                        url = videoInfo.item.videoUrl,
                        thumbnailUrl = videoInfo.item.thumbnailUrl,
                        mediaId = videoInfo.item.videoMD5,
                        onClick = { positionMs ->
                            VideoViewActivity.launch(context, videoInfo.item, positionMs)
                        }
                    )
                } else {
                    FeedVideoPreview(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .align(Alignment.Center)
                            .aspectRatio(ratio = videoInfo.item.aspectRatio().coerceIn(0.75f, 2f))
                            .clip(MaterialTheme.shapes.small),
                        url = videoInfo.item.videoUrl,
                        thumbnailUrl = videoInfo.item.thumbnailUrl,
                        mediaId = videoInfo.item.videoMD5,
                        onClick = { positionMs ->
                            VideoViewActivity.launch(context, videoInfo.item, positionMs)
                        }
                    )
                }
            }
        } else {
            if (mode == MediaDisplayMode.HIDE) {
                MediaPlaceholder(
                    icon = {
                        Icon(
                            imageVector = if (isSinglePhoto) Icons.Rounded.Photo else Icons.Rounded.PhotoLibrary,
                            contentDescription = stringResource(id = R.string.desc_image)
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.btn_open_photos, mediaCount))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val data = getPhotoViewData(medias, forumId, forumName, threadId, index = 0)
                        PhotoViewActivity.launch(context, data)
                    }
                )
            } else if (medias.first().isExpired) {
                ErrorImage(tip = stringResource(R.string.desc_expired_image))
            } else {
                val isLongPic = isSinglePhoto && medias.first().isLongPic == 1
                val singleAspectRatio = if (isLongPic) 2f else {
                    val w = medias.first().width
                    val h = medias.first().height
                    if (w > 0 && h > 0) (w.toFloat() / h).coerceIn(0.75f, 2f) else 2f
                }

                if (isSinglePhoto && mode == MediaDisplayMode.COMPACT && habitSettings.compactSingleAsGridCell) {
                    CompactMediaGrid(
                        modifier = Modifier.align(Alignment.CenterStart),
                        medias = medias,
                        widthFraction = frac,
                        forumId = forumId,
                        forumName = forumName,
                        threadId = threadId,
                        imageLoadType = habitSettings.imageLoadType,
                    )
                } else if (isSinglePhoto) {
                    val singleRatio = if (mode == MediaDisplayMode.COMPACT) 1f else singleAspectRatio
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .align(Alignment.Center)
                            .aspectRatio(singleRatio)
                    ) {
                        Row(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(MaterialTheme.shapes.small),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                NetworkImage(
                                    modifier = Modifier.matchParentSize(),
                                    imageUrl = medias[0].getPicUrl(habitSettings.imageLoadType),
                                    dimensions = IntSize(width = medias[0].width, height = medias[0].height),
                                    photoViewDataProvider = {
                                        getPhotoViewData(
                                            medias = medias.toImmutableList(),
                                            forumId = forumId,
                                            forumName = forumName,
                                            threadId = threadId,
                                            index = 0
                                        )
                                    },
                                )
                                if (medias[0].isLongPic == 1) {
                                    LongPicChip(modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    }
                } else if (mode == MediaDisplayMode.COMPACT) {
                    CompactMediaGrid(
                        modifier = Modifier.align(Alignment.CenterStart),
                        medias = medias,
                        widthFraction = frac,
                        forumId = forumId,
                        forumName = forumName,
                        threadId = threadId,
                        imageLoadType = habitSettings.imageLoadType,
                    )
                } else {
                    val hasMoreMedia = medias.size > MAX_PHOTO_IN_ROW
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .align(Alignment.Center)
                            .aspectRatio(3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(MaterialTheme.shapes.small),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (index in 0 until min(medias.size, MAX_PHOTO_IN_ROW)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    NetworkImage(
                                        modifier = Modifier.matchParentSize(),
                                        imageUrl = medias[index].getPicUrl(habitSettings.imageLoadType),
                                        dimensions = IntSize(width = medias[index].width, height = medias[index].height),
                                        photoViewDataProvider = {
                                            getPhotoViewData(
                                                medias = medias.toImmutableList(),
                                                forumId = forumId,
                                                forumName = forumName,
                                                threadId = threadId,
                                                index = index
                                            )
                                        },
                                    )
                                    if (medias[index].isLongPic == 1) {
                                        LongPicChip(modifier = Modifier.padding(6.dp))
                                    }
                                }
                            }
                        }
                        if (hasMoreMedia) {
                            MediaSizeBadge(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                size = medias.size,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val CompactMediaGridTileTarget = 110.dp
private val CompactMediaGridMaxRows = 2

/**
 * 紧凑档多图: 按可用宽度自动排成方格网格, 超出部分以「+N」角标呈现.
 */
@Composable
private fun CompactMediaGrid(
    modifier: Modifier = Modifier,
    medias: List<Media>,
    widthFraction: Float,
    forumId: Long,
    forumName: String,
    threadId: Long,
    imageLoadType: Int,
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth(widthFraction)) {
        val columns = (maxWidth.value / CompactMediaGridTileTarget.value)
            .roundToInt()
            .coerceIn(2, 5)
        val maxTiles = columns * CompactMediaGridMaxRows
        val plusN = (medias.size - maxTiles).coerceAtLeast(0)
        val visibleCount = if (plusN > 0) maxTiles else medias.size

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                medias.take(visibleCount).chunked(columns).forEachIndexed { rowIndex, rowMedias ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowMedias.forEachIndexed { colIndex, media ->
                            val index = rowIndex * columns + colIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                NetworkImage(
                                    modifier = Modifier.matchParentSize(),
                                    imageUrl = media.getPicUrl(imageLoadType),
                                    dimensions = IntSize(width = media.width, height = media.height),
                                    photoViewDataProvider = {
                                        getPhotoViewData(
                                            medias = medias.toImmutableList(),
                                            forumId = forumId,
                                            forumName = forumName,
                                            threadId = threadId,
                                            index = index,
                                        )
                                    },
                                )
                                if (media.isLongPic == 1) {
                                    LongPicChip(modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                        repeat(columns - rowMedias.size) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            if (plusN > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable {
                            PhotoViewActivity.launch(
                                context,
                                getPhotoViewData(
                                    medias = medias.toImmutableList(),
                                    forumId = forumId,
                                    forumName = forumName,
                                    threadId = threadId,
                                    index = maxTiles,
                                )
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$plusN",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LongPicChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(4.dp)
    ) {
        Text(
            text = stringResource(R.string.tip_long_pic),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun OriginThreadCard(
    originThreadInfo: ImmutableHolder<OriginThreadInfo>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val imageLoadType = LocalHabitSettings.current.imageLoadType
    val contentRenders = remember(originThreadInfo.item.tid) {
        originThreadInfo.get { content.buildRenders(imageLoadType) }
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onNotNull(onClick) { clickable(onClick = it) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            contentRenders.fastForEach {
                it.Render()
            }
        }
        ThreadMedia(
            forumId = originThreadInfo.get { fid },
            forumName = originThreadInfo.get { fname },
            threadId = originThreadInfo.get { tid.toLong() },
            medias = originThreadInfo.item.media,
            videoInfo = originThreadInfo.get { video_info }?.wrapImmutable()
        )
    }
}

@Composable
fun FeedCard(
    thread: ThreadItem,
    onClick: (ThreadItem) -> Unit,
    onLike: (ThreadItem) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadItem) -> Unit = onClick,
    onClickUser: (ThreadItem) -> Unit = {},
    onClickForum: ((ThreadItem) -> Unit)? = null, // Parse Null to Hide ForumInfo
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
    cardDivider: Boolean = false,
    onHide: ((ThreadItem) -> Unit)? = null,
    dislikeAction: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val (forumId, forumName, forumAvatar) = thread.simpleForum

    Card(
        header = {
            SharedTransitionUserHeader(
                user = thread.author,
                extraKey = thread.id,
                desc = remember { DateTimeUtils.getRelativeTimeString(context, thread.lastTimeMill) },
                onClick = { onClickUser(thread) },
                content = if (dislikeAction != null || onHide != null) {
                    {
                        dislikeAction?.invoke(this)
                        onHide?.let { HideOverflowMenu(thread = thread, onHide = it) }
                    }
                } else null
            )
        },
        content = {
            if (!thread.content.isNullOrEmpty()) {
                PbContentText(
                    text = thread.content,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp,
                    lineSpacing = 0.8.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            ThreadMedia(
                forumId = forumId,
                forumName = forumName,
                threadId = thread.id,
                medias = thread.medias ?: emptyList(),
                videoInfo = thread.video,
            )

            thread.originThreadInfo?.let {
                OriginThreadCard(
                    originThreadInfo = it,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onClickOriginThread(it.item) }
                        .padding(16.dp)
                )
            }

            if (onClickForum != null) {
                ForumInfoChip(
                    forumName = forumName,
                    avatarUrl = forumAvatar,
                    onClick = { onClickForum(thread) }
                )
            }
        },
        action = {
            ThreadActionButtonRow(
                modifier = Modifier.fillMaxWidth(),
                shares = thread.shareNum,
                replies = thread.replyNum,
                likes = thread.like.count,
                liked = thread.like.liked,
                onShareClicked = {
                    TiebaUtil.shareThread(context, thread.title, thread.id)
                },
                onReplyClicked = { onClickReply(thread) },
                onAgreeClicked = { onLike(thread) }
            )
        },
        onClick = { onClick(thread) },
        modifier = modifier
            .onCase(cardDivider) { cardBottomDivider(DividerDefaults.color) },
    )
}

@Composable
private fun HideOverflowMenu(thread: ThreadItem, onHide: (ThreadItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.btn_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.menu_hide_thread)) },
                onClick = {
                    expanded = false
                    onHide(thread)
                },
            )
        }
    }
}

/** 屏蔽 (黑名单) 帖子在列表中的占位提示. */
val ThreadBlockedTip: @Composable BoxScope.() -> Unit = {
    BlockTip(modifier = Modifier.padding(horizontal = CardHorizontalSpacing, vertical = 4.dp)) {
        Text(text = stringResource(id = R.string.tip_blocked_thread))
    }
}

@Composable
private fun ThreadHiddenTip(
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideContentColorTextStyle(
            contentColor = MaterialTheme.colorScheme.onSurface,
            textStyle = MaterialTheme.typography.bodyMedium,
        ) {
            Text(
                text = stringResource(id = R.string.tip_hidden_thread),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUndo) {
                Text(text = stringResource(id = R.string.button_undo))
            }
        }
    }
}

/**
 * 帖子列表项的统一占位包装: 处理「已隐藏」与「被屏蔽」两种情况.
 *
 * - 已隐藏: 当 [hideBlockedContent] 为真时完全隐藏, 否则显示带「撤销」按钮的占位.
 * - 被屏蔽 (黑名单): 与旧行为一致, 按 [hideBlockedContent] 显示/彻底隐藏.
 */
@Composable
fun FeedThreadItem(
    thread: ThreadItem,
    hideBlockedContent: Boolean,
    modifier: Modifier = Modifier,
    onUndoHidden: (ThreadItem) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    when {
        thread.hidden && hideBlockedContent -> Unit

        thread.hidden -> ThreadHiddenTip(
            onUndo = { onUndoHidden(thread) },
            modifier = modifier,
        )

        thread.blocked -> BlockableContent(
            blocked = true,
            blockedTip = ThreadBlockedTip,
            hideBlockedContent = hideBlockedContent,
            modifier = modifier,
            content = content,
        )

        else -> Box(modifier = modifier, content = content)
    }
}

@Composable
@NonRestartableComposable
fun FeedVideoShutter(
    thumbnailUrl: String,
    modifier: Modifier = Modifier,
    isPipMode: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (!isPipMode) {
        VideoThumbnail(modifier = modifier.fillMaxSize(), thumbnailUrl, contentScale)
    } else {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PictureInPictureAlt,
                contentDescription = stringResource(R.string.btn_picture_in_picture),
                modifier = Modifier.size(Sizes.Large),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun FeedVideoPreview(
    url: String,
    thumbnailUrl: String,
    mediaId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (positionMs: Long) -> Unit = {},
) {
    val previewState = LocalVideoPreviewState.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isFullyVisible by remember { mutableStateOf(false) }

    val shutter = remember {
        movableContentOf<Boolean> { FeedVideoShutter(thumbnailUrl, isPipMode = it, contentScale = contentScale) }
    }

    Box(
        modifier = modifier
            .onNotNull(previewState) {
                onVisibilityChanged(
                    minDurationMs = VIDEO_MIN_VISIBLE_DURATION_MS,
                    minFractionVisible = 1f, // 100%
                    callback = { isFullyVisible = it }
                )
            }
            .clickableNoIndication {
                previewState?.onEnterVideoView(mediaId)
                onClick(MediaUtil.getCurrentPositionMs(player))
            },
    ) {
        if (previewState == null || previewState.isInVideoViewMode) {
            shutter(previewState?.videoViewMediaId == mediaId && previewState.isInPipMode)
            return@Box
        }

        if (player != null) {
            PreviewVideoPlayer(
                player = player,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                shutter = { shutter(false) },
            )
        } else {
            shutter(false)
        }

        LaunchedEffect(isFullyVisible) {
            if (!isFullyVisible) return@LaunchedEffect
            player = player ?: previewState.preparePreview(url, mediaId)
            previewState.play(mediaId, player)
        }

        if (isFullyVisible && player != null) {
            DisposableEffect(player) {
                onDispose {
                    previewState.disposePreview(mediaId, player)
                    player = null
                }
            }
        }
    }
}

@Composable
private fun ActionBtnPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Button",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
        )
    }
}

@Preview("FeedCardPreview")
@Composable
private fun FeedCardPreview() = TiebaLiteTheme {
    Surface {
        FeedCard(
            thread = ThreadItem(
                author = Author(0, name = "FeedCardPreview", avatarUrl = ""),
                title = "预览",
                lastTimeMill = System.currentTimeMillis(),
                replyNum = 99999,
                shareNum = 20,
                simpleForum = SimpleForum(-1, "Test", "")
            ),
            onClick = {},
            onLike = {},
        )
    }
}
