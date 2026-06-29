@file:androidx.annotation.OptIn(UnstableApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.material3.buttons.MuteButton
import androidx.media3.ui.compose.material3.buttons.RepeatButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import androidx.media3.ui.compose.material3.indicator.PositionAndDurationText
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.theme.Grey100
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons.ContentScaleButton
import com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons.LabeledProgressSlider
import com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons.MediaFormatsButton
import com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons.PlayPauseButton
import com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons.PlaybackSpeedBottomSheetButton

@Composable
fun retainVideoPlayer(
    initialMediaItem: MediaItem,
    playWhenReady: Boolean = true,
): Player {
    val context = LocalContext.current
    val player = retain { ExoPlayer.Builder(context.applicationContext).build() }
    var wasPlaying by retain { mutableStateOf(playWhenReady) }

    LifecycleStartEffect(Unit) {
        if (wasPlaying && !player.isPlaying && player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            player.playWhenReady = true
        }

        onStopOrDispose {
            wasPlaying = player.isPlaying
            if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
                player.pause()
            }
        }
    }

    // Initialize each player only once after we retain it.
    // If the uri (and therefore the player) change, we need to dispose the old player
    // and initialize the new one. Likewise, the player needs to be disposed of when
    // it stops being retained.
    RetainedEffect(player) {
        val factory = ProgressiveMediaSource.Factory(MediaCache.Factory(context))
        player.setMediaSource(factory.createMediaSource(initialMediaItem))
        player.prepare()

        onRetire {
            if (!player.isReleased) player.release()
        }
    }
    return player
}

/**
 * Video player component, combining a [ContentFrame] for displaying player content with
 * gesture-driven media controls and customizable top controls.
 *
 * @param player The [Player] instance to be controlled and whose content is displayed.
 * @param modifier The [Modifier] to be applied to the outer [Box].
 * @param contentScale The scaling mode to apply to the content within the [ContentFrame].
 * @param gestureState The [PlayerGestureState] that manages gesture interactions such as seek
 *   using dragging and tapping gestures.
 * @param thumbnailUrl Optional URL to be displayed as a shutter over the content. If `null` or
 *   empty, a black [Box] is used.
 * @param topControls A composable aligned with [Alignment.TopCenter].
 */
@Composable
internal fun VideoPlayer(
    player: Player?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    gestureState: PlayerGestureState = rememberPlayerGestureState(player),
    thumbnailUrl: String? = null,
    topControls: (@Composable BoxScope.() -> Unit)? = {
        TopControls(player, modifier = Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets))
    },
) {
    var currentContentScale by remember { mutableStateOf(contentScale) }

    Box(modifier = modifier) {
        ContentFrame(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            contentScale = currentContentScale,
        ) {
            val shutterModifier = Modifier.fillMaxSize().background(Color.Black)
            if (!thumbnailUrl.isNullOrEmpty()) {
                VideoThumbnail(
                    modifier = shutterModifier,
                    thumbnailUrl = thumbnailUrl,
                    contentScale = currentContentScale,
                    onClick = { Util.handlePlayButtonAction(player) }
                )
            } else {
                Box(modifier = shutterModifier) // PlayerDefaults.Shutter()
            }
        }

        MediaControlGestures(Modifier.matchParentSize(), gestureState)

        CompositionLocalProvider(LocalPlayerGestureState provides gestureState) {
            AnimatedVisibility(gestureState.controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (topControls != null) {
                        Box(modifier = Modifier.align(Alignment.TopStart), content = topControls)
                    }

                    CenterControls(player, modifier = Modifier.align(Alignment.Center))

                    BottomControlsWithLabeledProgress(
                        player = player,
                        contentScale = currentContentScale,
                        onContentScaleSelected = { currentContentScale = it },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .windowInsetsPadding(NavigationBarDefaults.windowInsets),
                    )
                }
            }
        }
    }
}

/**
 * Control component typically placed at the top of a video player.
 *
 * This small composable contains a horizontal row of Material3 [IconButton], providing back
 * navigation, download, mute toggle, and media info dialog button.
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to this top controls composable.
 * @param onBack An optional callback invoked when the back button is clicked.
 * @param onDownload An optional callback invoked when the download button is clicked. If `null`,
 *   the download button is not shown.
 */
@Composable
fun TopControls(
    player: Player?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
) {
    val topButtonColors = IconButtonDefaults.filledTonalIconButtonColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawControlsScrim()
            .padding(16.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ActionItem(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.btn_close),
                colors = IconButtonDefaults.filledIconButtonColors(),
                onClick = onBack,
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))
        if (onDownload != null) {
            ActionItem(
                onClick = onDownload,
                icon = Icons.Rounded.Download,
                contentDescription = stringResource(R.string.btn_download),
                colors = topButtonColors,
            )
        }
        MuteButton(player, colors = topButtonColors)
        MediaFormatsButton(player, colors = topButtonColors)
    }
}

/**
 * Control component typically placed at the center of a video player.
 *
 * A [androidx.media3.ui.compose.material3.PlayerDefaults.CenterControls] without previous and
 * next button.
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to this composable.
 * @param buttonColors [IconButtonColors] that will be used to resolve the colors used for all
 *   control button.
 */
@Composable
private fun CenterControls(
    player: Player?,
    modifier: Modifier = Modifier,
    buttonColors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.primary
    ),
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeekBackButton(player, Modifier.size(50.dp), colors = buttonColors)
        PlayPauseButton(player, Modifier.size(64.dp), colors = buttonColors)
        SeekForwardButton(player, Modifier.size(50.dp), colors = buttonColors)
    }
}

@Composable
private fun BottomControlsWithLabeledProgress(
    player: Player?,
    contentScale: ContentScale,
    onContentScaleSelected: (ContentScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestureState = LocalPlayerGestureState.current
    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val iconButtonColors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
    val presetsContentScale = remember {
        listOf(ContentScale.Fit, ContentScale.Crop, ContentScale.FillHeight, ContentScale.FillWidth, ContentScale.FillBounds)
    }

    Column(
        modifier = Modifier
            .drawControlsScrim(reverse = true)
            .clickableNoIndication { /** Block Gestures */ }
            .then(modifier),
    ) {
        LabeledProgressSlider(
            player = player,
            modifier = Modifier
                .fillMaxWidth()
                .onNotNull(gestureState) {
                    reportPointerDown { isDown -> it.showControls(autoHide = !isDown) }
                }
                .padding(horizontal = 16.dp)
        )

        if (windowSize.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp), // Make last button visually aligned
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionAndDurationText(player = player, color = contentColor)
            Spacer(Modifier.weight(1f))
            PlaybackSpeedBottomSheetButton(
                player = player,
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            )
            ContentScaleButton(
                contentScale = contentScale,
                onContentScaleSelected = onContentScaleSelected,
                presetsContentScale = presetsContentScale,
                colors = iconButtonColors,
            )
            RepeatButton(
                player = player,
                toggleModeSequence = listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE),
                colors = iconButtonColors,
            )
            FullScreenButton(colors = iconButtonColors)
        }
    }
}

@Composable
private fun FullScreenButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    val context = LocalContext.current
    ActionItem(
        modifier = modifier,
        onClick = {
            with(context.findActivity()!!) {
                if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        },
        icon = Icons.Rounded.ScreenRotation,
        contentDescription = stringResource(id = R.string.btn_full_screen),
        enabled = enabled,
        colors = colors,
    )
}

@Composable
fun VideoThumbnail(
    modifier: Modifier = Modifier,
    thumbnailUrl: String?,
    contentScale: ContentScale = ContentScale.FillWidth,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl != null) {
            NetworkImage(
                imageUrl = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }

        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(id = R.string.btn_play),
            modifier = Modifier.size(48.dp),
            tint = Grey100
        )
    }
}

private fun Modifier.drawControlsScrim(scrim: Color = Color.Black, reverse: Boolean = false): Modifier {
    val scrimGradientColors = listOf(scrim.copy(alpha = 0.8f), scrim.copy(alpha = 0.3f), Color.Transparent)
    val backgroundBrush = Brush.verticalGradient(colors = scrimGradientColors)
    return this then Modifier.drawWithCache {
        onDrawBehind {
            if (reverse) {
                rotate(degrees = 180.0f) { drawRect(brush = backgroundBrush) }
            } else {
                drawRect(brush = backgroundBrush)
            }
        }
    }
}

private fun Modifier.reportPointerDown(onPointerDownChange: (Boolean) -> Unit): Modifier =
    this.pointerInput(onPointerDownChange) {
        awaitPointerEventScope {
            var isDown = false
            while (true) {
                val event = awaitPointerEvent()
                val currentlyPressed = event.changes.any { it.pressed }
                if (isDown != currentlyPressed) {
                    isDown = currentlyPressed
                    onPointerDownChange(isDown)
                }
            }
        }
    }
