package com.huanchengfly.tieba.post.ui.widgets.compose.video.buttons

import android.media.MediaCodecInfo.CodecProfileLevel
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.CodecSpecificDataUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.PlayerStateObserver
import androidx.media3.ui.compose.state.observeState
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import com.huanchengfly.tieba.post.components.MediaCache.getBdMediaId
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogContentPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.CONTROLS_VISIBILITY_TIMEOUT_MS
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalPlayerGestureState
import kotlinx.coroutines.launch

// Video format and Audio format
private typealias MediaFormats = Pair<Format, Format?>

/**
 * A Material3 [IconButton][androidx.compose.material3.IconButton] that toggles media format
 * info dialog.
 *
 * @param player The [Player] to control.
 * @param modifier The [Modifier] to be applied to the button.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconButtonColors].
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MediaFormatsButton(
    player: Player?,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    val coroutineScope = rememberCoroutineScope()
    val gestureState = LocalPlayerGestureState.current
    val state = rememberMediaFormatButtonState(player)
    val dialogState = rememberDialogState()
    var mediaFormats by remember { mutableStateOf<MediaFormats?>(null) }
    var mediaSize by remember { mutableLongStateOf(-1) }

    ActionItem(
        icon = Icons.Rounded.Info,
        modifier = modifier,
        contentDescription = stringResource(R.string.title_media_info),
        colors = colors,
        enabled = state.isEnabled && gestureState?.isEnabled ?: true,
        onClick = {
            val item = player?.currentMediaItem?.localConfiguration?.uri
            if (item != null && mediaSize == -1L) {
                coroutineScope.launch {
                    mediaSize = MediaCache.getContentLength(item.getBdMediaId(), item.toString())
                }
            }
            mediaFormats = state.getMediaFormats()
            dialogState.show = mediaFormats != null
            gestureState?.showControls(autoHide = !dialogState.show)
        }
    )

    mediaFormats?.let {
        MediaFormatDialog(
            mediaFormats = it,
            mediaSize = mediaSize,
            dialogState = dialogState,
            onDismiss = {
                dialogState.show = false
                gestureState?.autoHideControls(timeout = CONTROLS_VISIBILITY_TIMEOUT_MS / 3)
            }
        )
    }
}

@UnstableApi
@Composable
private fun MediaFormatDialog(
    mediaFormats: MediaFormats,
    mediaSize: Long,
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    onDismiss: (() -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = AnyPopDialogProperties(direction = DirectionState.CENTER),
        onDismiss = onDismiss,
        buttons = {
            DialogNegativeButton(text = stringResource(R.string.btn_close))
        }
    ) {
        val context = LocalContext.current
        val isWindowHeightCompact by derivedMediaQuery {
            windowHeight <= WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp
        }

        val (videoInfos, audioInfos) = remember(mediaFormats, mediaSize) {
            val (videoFormat, audioFormat) = mediaFormats
            val video = videoFormat.run {
                listOf(
                    R.string.text_media_codec to (getVideoProfileName() ?: "Null"),
                    R.string.text_media_resolution to "${width}x${height}",
                    R.string.text_media_fps to "${frameRate.toInt()} fps",
                    R.string.text_media_bitrate to if (bitrate > 0) "${bitrate / 1000} kbps" else "Null",
                    R.string.text_media_size to if (mediaSize > 0) {
                        Formatter.formatFileSize(context, mediaSize)
                    } else {
                        context.getString(R.string.text_loading)
                    },
                )
            }
            val audio = audioFormat?.run {
                listOf(
                    R.string.text_media_codec to (getAudioProfileName() ?: "Null"),
                    R.string.text_media_channel_count to "$channelCount ${context.getString(R.string.text_media_channel_count)}",
                    R.string.text_media_sample_rate to "$sampleRate Hz",
                    R.string.text_media_bitrate to if (bitrate > 0) "${bitrate / 1000} kbps" else "Null",
                )
            }
            video to audio
        }

        val infoListContent = remember {
            movableContentOf<String, List<Pair<Int, String>>> { title, info ->
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )

                info.fastForEach { (labelRes, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(labelRes), modifier = Modifier.widthIn(min = 96.dp))
                        Text(text = value)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = DefaultDialogContentPadding + 16.dp)
                .onCase(isWindowHeightCompact) {
                    fillMaxHeight().verticalScroll(rememberScrollState())
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            infoListContent(stringResource(R.string.desc_video), videoInfos)

            if (audioInfos != null) {
                Spacer(modifier = Modifier.height(6.dp))
                infoListContent(stringResource(R.string.desc_audio), audioInfos)
            }
        }
    }
}

/**
 * Remember the value of [MediaFormatState] created based on the passed [Player] and launch a
 * coroutine to listen to [Player's][Player] changes. If the [Player] instance changes between
 * compositions, produce and remember a new value.
 */
@UnstableApi
@Composable
fun rememberMediaFormatButtonState(player: Player?): MediaFormatState {
    val state = remember(player) { MediaFormatState(player) }
    LaunchedEffect(player) { state.observe() }
    return state
}

@UnstableApi
class MediaFormatState(
    private val player: Player?,
) {
    var isEnabled by mutableStateOf(false)
        private set

    private val playerStateObserver: PlayerStateObserver? =
        player?.observeState(
            Player.EVENT_TRACKS_CHANGED,
            Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
        ) {
            isEnabled = player.isCommandAvailable(Player.COMMAND_GET_TRACKS)
        }

    fun getMediaFormats(): MediaFormats? {
        if (player!!.isCommandAvailable(Player.COMMAND_GET_TRACKS)) {
            val track = player.currentTracks ?: return null
            return track.getSelectedTrackFormat(C.TRACK_TYPE_VIDEO)?.let {
                it to track.getSelectedTrackFormat(C.TRACK_TYPE_AUDIO)
            }
        }
        return null
    }

    /**
     * Subscribes to updates from [Player.Events] and listens to
     * * [Player.EVENT_TRACKS_CHANGED] in order to determine the [Player.getCurrentTracks].
     * * [Player.EVENT_AVAILABLE_COMMANDS_CHANGED] in order to determine whether the button should be
     *   enabled, i.e. respond to user input.
     */
    suspend fun observe() {
        playerStateObserver?.observe()
    }
}

/**
 * Returns the media format of selected track
 *
 * @param trackType [C.TrackType]
 * */
@UnstableApi
private fun Tracks.getSelectedTrackFormat(trackType: Int): Format? {
    for (groupInfo in groups) {
        if (groupInfo.type == trackType) {
            for (i in 0 until groupInfo.mediaTrackGroup.length) {
                if (groupInfo.isTrackSelected(i)) return groupInfo.getTrackFormat(i)
            }
        }
    }
    return null
}

@UnstableApi
private fun Format.getVideoProfileName(): String? {
    val (profile, level) = CodecSpecificDataUtil.getCodecProfileAndLevel(this) ?: return null
    val profileName = when (profile) {
        // --- AVC / H.264 Profiles ---
        CodecProfileLevel.AVCProfileBaseline -> "AVC Baseline"
        CodecProfileLevel.AVCProfileMain -> "AVC Main"
        CodecProfileLevel.AVCProfileHigh,
        CodecProfileLevel.AVCProfileHigh422,
        CodecProfileLevel.AVCProfileHigh444 -> "AVC High"
        // --- HEVC / H.265 Profiles ---
        CodecProfileLevel.HEVCProfileMain -> "HEVC Main"
        CodecProfileLevel.HEVCProfileMain10 -> "HEVC Main 10"
        // --- VP9 Profiles ---
        CodecProfileLevel.VP9Profile0 -> "VP9 0"
        CodecProfileLevel.VP9Profile1 -> "VP9 1"
        CodecProfileLevel.VP9Profile2 -> "VP9 2"
        CodecProfileLevel.VP9Profile3 -> "VP9 3"
        // --- AV1 Profiles ---
        else -> return null // ...
    }

    val levelName = when (level) {
        // --- AVC Levels ---
        CodecProfileLevel.AVCLevel2 -> "2.0"
        CodecProfileLevel.AVCLevel3 -> "3.0"
        CodecProfileLevel.AVCLevel31 -> "3.1"
        CodecProfileLevel.AVCLevel4 -> "4.0"
        CodecProfileLevel.AVCLevel41 -> "4.1"
        CodecProfileLevel.AVCLevel5 -> "5.0"
        CodecProfileLevel.AVCLevel51 -> "5.1"
        CodecProfileLevel.AVCLevel52 -> "5.2"
        // --- HEVC Levels ---
        CodecProfileLevel.HEVCMainTierLevel1 -> "1.0 (Main)"
        CodecProfileLevel.HEVCHighTierLevel1 -> "1.0 (High)"
        CodecProfileLevel.HEVCMainTierLevel2 -> "2.0 (Main)"
        CodecProfileLevel.HEVCHighTierLevel2 -> "2.0 (High)"
        CodecProfileLevel.HEVCMainTierLevel21 -> "2.1 (Main)"
        CodecProfileLevel.HEVCHighTierLevel21 -> "2.1 (High)"
        CodecProfileLevel.HEVCMainTierLevel3 -> "3.0 (Main)"
        CodecProfileLevel.HEVCHighTierLevel3 -> "3.0 (High)"
        CodecProfileLevel.HEVCMainTierLevel31 -> "3.1 (Main)"
        CodecProfileLevel.HEVCHighTierLevel31 -> "3.1 (High)"
        CodecProfileLevel.HEVCMainTierLevel4 -> "4.0 (Main)"
        CodecProfileLevel.HEVCHighTierLevel4 -> "4.0 (High)"
        CodecProfileLevel.HEVCMainTierLevel41 -> "4.1 (Main)"
        CodecProfileLevel.HEVCHighTierLevel41 -> "4.1 (High)"
        CodecProfileLevel.HEVCMainTierLevel5 -> "5.0 (Main)"
        CodecProfileLevel.HEVCHighTierLevel5 -> "5.0 (High)"
        CodecProfileLevel.HEVCMainTierLevel51 -> "5.1 (Main)"
        CodecProfileLevel.HEVCHighTierLevel51 -> "5.1 (High)"
        CodecProfileLevel.HEVCMainTierLevel52 -> "5.2 (Main)"
        CodecProfileLevel.HEVCHighTierLevel52 -> "5.2 (High)"
        // --- VP9 Levels ---
        CodecProfileLevel.VP9Level1 -> "1.0"
        CodecProfileLevel.VP9Level11 -> "1.1"
        CodecProfileLevel.VP9Level2 -> "2.0"
        CodecProfileLevel.VP9Level21 -> "2.1"
        CodecProfileLevel.VP9Level3 -> "3.0"
        CodecProfileLevel.VP9Level31 -> "3.1"
        CodecProfileLevel.VP9Level4 -> "4.0"
        CodecProfileLevel.VP9Level41 -> "4.1"
        CodecProfileLevel.VP9Level5 -> "5.0"
        CodecProfileLevel.VP9Level51 -> "5.1"

        else -> return null // ...
    }

    return "$profileName@L$levelName"
}

@UnstableApi
private fun Format.getAudioProfileName(): String? {
    val (profile, _) = CodecSpecificDataUtil.getCodecProfileAndLevel(this) ?: return null

    return when (profile) {
        // --- AAC (Advanced Audio Coding) Profiles ---
        CodecProfileLevel.AACObjectMain -> "AAC Main"
        CodecProfileLevel.AACObjectLC -> "AAC LC"
        CodecProfileLevel.AACObjectSSR -> "AAC SSR"
        CodecProfileLevel.AACObjectLTP -> "AAC LTP"
        CodecProfileLevel.AACObjectHE -> "AAC HE"
        CodecProfileLevel.AACObjectScalable -> "AAC Scalable"
        CodecProfileLevel.AACObjectERLC -> "AAC ER LC"
        CodecProfileLevel.AACObjectLD -> "AAC LD"
        CodecProfileLevel.AACObjectELD -> "AAC ELD"
        CodecProfileLevel.AACObjectXHE -> "AAC xHE"

        // --- DTS Profiles ---
        CodecProfileLevel.DTS_HDProfileMA -> "DTS-HD MA"
        CodecProfileLevel.DTS_HDProfileHRA -> "DTS-HD MA"

        // --- Other Common Audio ---
        CodecProfileLevel.MPEG4ProfileSimple -> "MPEG4 Audio"

        else -> null // ...
    }
}
