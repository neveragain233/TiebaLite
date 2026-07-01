@file:androidx.annotation.OptIn(UnstableApi::class)

package com.huanchengfly.tieba.post.activities

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.observeState
import coil3.imageLoader
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.components.MediaCache.BD_VIDEO_HOST
import com.huanchengfly.tieba.post.components.MediaCache.getBdVideoMD5
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.video.TopControls
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.initialize
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberPlayerGestureState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.retainVideoPlayer
import com.huanchengfly.tieba.post.utils.DownloadUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

class VideoViewActivity: AppCompatActivity() {

    private var downloadId: Long by mutableLongStateOf(-1)
    private var downloadBroadcastReceiver: DownloadBroadcastReceiver? = null

    private inner class DownloadBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE && downloadId > 0 &&
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId
            ) {
                lifecycleScope.launch {
                    if (DownloadUtil.queryById(downloadId) == DownloadManager.STATUS_SUCCESSFUL) {
                        val dest = DownloadUtil.DEFAULT_DOWNLOAD_DIR.toString()
                        context.toastShort(R.string.toast_video_downloaded, dest)
                    }
                }
            }
        }
    }

    private val darkColorSchemeFlow by unsafeLazy {
        ThemeUtil.savedColorSchemeFlow(
            themeSettings = App.INSTANCE.settingRepository.themeSettings,
            context = this
        )
        .map { ExtendedColorScheme(colorScheme = it.darkColor, darkTheme = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)
        downloadId = savedInstanceState?.getLong(KEY_DOWNLOAD_ID, -1) ?: -1
        val data = intent.data ?: throw NullPointerException("No video provided!")
        var thumbnailUrl by mutableStateOf(intent.getStringExtra(EXTRA_THUMBNAIL))

        setContent {
            val player = retainVideoPlayer(initialMediaItem = MediaItem.fromUri(data))
            val gestureState = rememberPlayerGestureState(player)
            val colorScheme by darkColorSchemeFlow.collectAsStateWithLifecycle(DefaultDarkColors)

            TiebaLiteTheme(colorSchemeExt = colorScheme) {
                VideoPlayer(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    gestureState = gestureState,
                    thumbnailUrl = thumbnailUrl,
                    topControls = { pipState ->
                        val downloadable by collectDownloadableStatusAsState(downloadId)
                        TopControls(
                            player = player,
                            modifier = Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets),
                            pipState = pipState,
                            onBack = onBackPressedDispatcher::onBackPressed,
                            onDownload = ::onDownloadClicked.takeIf { downloadable },
                        )
                    },
                )
            }

            ObservePlayerError(player)

            LaunchedEffect(Unit) {
                callbackFlow {
                    val consumer = Consumer<Intent> { trySend(it) }
                    addOnNewIntentListener(consumer)
                    awaitClose { removeOnNewIntentListener(consumer) }
                }
                .collectLatest { newIntent ->
                    val newVideo = newIntent.data?.normalizeScheme() ?: return@collectLatest
                    if (newVideo != player.currentMediaItem?.localConfiguration?.uri) {
                        thumbnailUrl = newIntent.getStringExtra(EXTRA_THUMBNAIL)
                        player.initialize(applicationContext, MediaItem.fromUri(newVideo))
                        player.playWhenReady = true
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(KEY_DOWNLOAD_ID, downloadId)
        super.onSaveInstanceState(outState)
    }

    private fun enqueueDownload(videoUri: Uri) {
        if (downloadId > 0) {
            DownloadUtil.downloadManager.remove(downloadId)
        }
        if (downloadBroadcastReceiver == null) {
            downloadBroadcastReceiver = DownloadBroadcastReceiver().also {
                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                ContextCompat.registerReceiver(this, it, filter, ContextCompat.RECEIVER_EXPORTED)
            }
        }
        runCatching {
            val destination = File(DownloadUtil.DEFAULT_DOWNLOAD_DIR, videoUri.bdFileName)
            downloadId = DownloadUtil.downloadVideo(videoUri, destination)
        }
        .onFailure { e ->
            toastShort(R.string.toast_exception, e.getErrorMessage())
        }
        .onSuccess { toastShort(R.string.toast_download_manager_enqueued) }
    }

    private fun onDownloadClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enqueueDownload(videoUri = intent.data!!)
        } else {
            val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            lifecycleScope.launch {
                askPermission(R.string.tip_permission_storage_download, *permission)
                    .onDenied {
                        toastShort(R.string.toast_no_permission_download_video)
                    }
                    .onGranted { enqueueDownload(videoUri = intent.data!!) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadBroadcastReceiver?.let { unregisterReceiver(it) }
    }

    companion object {
        private const val KEY_DOWNLOAD_ID = "com.huanchengfly.tieba.post.VideoViewActivity.DOWNLOAD_ID"

        const val EXTRA_THUMBNAIL = "video_thumbnail"

        fun launch(context: Context, videoUrl: String, thumbnailUrl: String? = null) {
            val data = Uri.parse(videoUrl).normalizeScheme()

            // Check tb-video is unauthorized
            if (data.host == BD_VIDEO_HOST && videoUrl.endsWith(".mp4")) {
                context.toastShort(R.string.desc_expired_video_link)
                return
            }

            // Free more memory now
            context.imageLoader.memoryCache?.run { trimToSize((maxSize * 0.2).roundToLong()) }

            context.goToActivity<VideoViewActivity> {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this.data = data
                thumbnailUrl?.let { putExtra(EXTRA_THUMBNAIL, it) }
            }
        }

        fun launch(context: Context, videoInfo: VideoInfo) {
            launch(context, videoInfo.videoUrl, videoInfo.thumbnailUrl)
        }

        @Composable
        private fun ObservePlayerError(player: Player) {
            val context = LocalContext.current
            val playerObserver = remember(player) {
                player.observeState(
                    Player.EVENT_PLAYER_ERROR,
                ) {
                    player.playerError?.let { e ->
                        context.toastShort(R.string.toast_exception, e.getErrorMessage())
                    }
                }
            }
            LaunchedEffect(player) {
                playerObserver.observe()
            }
        }

        @Composable
        private fun collectDownloadableStatusAsState(
            downloadId: Long,
            lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
        ): State<Boolean> = produceState(initialValue = false, downloadId, lifecycleOwner) {
            if (downloadId < 0) {
                value = true
            } else {
                val query = DownloadManager.Query().setFilterById(downloadId)
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        when(DownloadUtil.queryStatus(query)) {
                            null,
                            DownloadManager.STATUS_FAILED -> value = true

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                value = false
                                break
                            }

                            else -> value = false
                        }
                        delay(duration = 2.seconds)
                    }
                }
            }
        }

        private val Uri.bdFileName: String
            get() = "${getBdVideoMD5() ?: hashCode()}.mp4"
    }
}