package com.huanchengfly.tieba.post

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.translucentBackground
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.media.ExoPlayerPool
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.ui.models.settings.AutoUpdateCheckInterval
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.update.UpdateDownloadWorker
import com.huanchengfly.tieba.post.update.UpdateManager
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Immutable
data class MainUiState(
    val habitSettings: HabitSettings? = null,
    val uiSettings: UISettings? = null,
    val autoSignRestricted: Boolean = false,
    val themeColor: ExtendedColorScheme = ThemeUtil.getRawTheme(),
)

/** 自动检查更新后需要展示的应用内提示. */
data class AutoUpdatePrompt(
    val info: UpdateManager.UpdateInfo,
    val cachedFile: File? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val settingsRepository: SettingsRepository,
    private val forumRepo: ForumRepository,
    private val threadRepo: PbPageRepository
) : ViewModel() {

    val account: SharedFlow<Account?> = AccountUtil.getInstance().currentAccount

    val previewInfoFlow = ClipBoardLinkDetector.previewInfoStateFlow

    private var autoUpdateCheckJob: Job? = null
    private var autoUpdateCheckStarted = false
    private val _autoUpdatePrompt = MutableStateFlow<AutoUpdatePrompt?>(null)
    val autoUpdatePrompt = _autoUpdatePrompt.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.habitSettings,
        settingsRepository.uiSettings,
        settingsRepository.signConfig.map { it.autoSign },
        ThemeUtil.getExtendedColorFlow(settingsRepository, context),
    ) { habitSettings, uiSettings, autoSign, themeColor ->
        // Show warning dialog when background activity is restricted
        val autoSignRestricted = autoSign && !context.isIgnoringBatteryOptimizations()
        MainUiState(habitSettings, uiSettings, autoSignRestricted, themeColor)
    }
    .flowOn(Dispatchers.Default)
    .stateInViewModel(initialValue = MainUiState())

    /**
     * Cropped wallpaper file of [Theme.TRANSLUCENT], **null** when current theme is not translucent.
     * */
    val translucentThemeBackground: StateFlow<File?> = settingsRepository.themeSettings
        .map {
            if (it.theme == Theme.TRANSLUCENT && it.transBackground != null) {
                context.translucentBackground(it.transBackground)
            } else {
                null
            }
        }
        .stateInViewModel(initialValue = null)

    private val privacySettings: Settings<PrivacySettings> = settingsRepository.privacySettings

    private val _playerPool: Lazy<ExoPlayerPool> = lazy { ExoPlayerPool.defaultExoPlayerPool(context) }
    val playerPool: ExoPlayerPool
        get() = _playerPool.value

    fun onAppLaunched() {
        if (autoUpdateCheckStarted || autoUpdateCheckJob?.isActive == true) return
        autoUpdateCheckStarted = true
        autoUpdateCheckJob = viewModelScope.launch { checkForUpdateAutomatically() }
    }

    fun dismissAutoUpdatePrompt() {
        _autoUpdatePrompt.value = null
    }

    fun downloadAutoUpdate() {
        val prompt = _autoUpdatePrompt.value ?: return
        if (prompt.cachedFile == null) {
            UpdateDownloadWorker.enqueue(context, prompt.info)
            Toast.makeText(context, R.string.toast_update_background_start, Toast.LENGTH_SHORT).show()
        }
        _autoUpdatePrompt.value = null
    }

    fun installAutoUpdate() {
        val prompt = _autoUpdatePrompt.value ?: return
        val file = prompt.cachedFile ?: return
        val needPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
        if (needPermission) {
            Toast.makeText(context, R.string.update_install_permission_needed, Toast.LENGTH_LONG).show()
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        if (UpdateManager.installApk(context, file)) {
            _autoUpdatePrompt.value = null
        } else {
            Toast.makeText(context, R.string.update_install_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun checkForUpdateAutomatically() {
        val settings = settingsRepository.updateSettings.snapshot()
        val interval = settings.autoUpdateCheckInterval
        val now = System.currentTimeMillis()
        if (interval != AutoUpdateCheckInterval.EVERY_LAUNCH &&
            now - settings.lastAutoUpdateCheckAt < TimeUnit.DAYS.toMillis(interval.days.toLong())
        ) {
            return
        }
        settingsRepository.updateSettings.save {
            it.copy(lastAutoUpdateCheckAt = now)
        }
        if (!settingsRepository.uiSettings.snapshot().setupFinished) return

        val info = runCatching { UpdateManager.checkForUpdate() }.getOrNull() ?: return
        val cachedFile = UpdateManager.updateApkFile(context, info)
        val isDownloaded = cachedFile.exists() &&
                (info.apkSize <= 0L || cachedFile.length() == info.apkSize)
        _autoUpdatePrompt.value = when {
            isDownloaded -> AutoUpdatePrompt(info, cachedFile)
            settings.backgroundDownload -> {
                UpdateDownloadWorker.enqueue(context, info)
                null
            }
            else -> AutoUpdatePrompt(info)
        }
    }

    fun onCheckClipBoard() {
        viewModelScope.launch {
            val setupFinished = uiState.value.uiSettings?.setupFinished == true
            if (setupFinished && privacySettings.snapshot().readClipBoardLink) {
                ClipBoardLinkDetector.checkClipBoard(context, forumRepo, threadRepo)
            }
        }
    }

    fun onClipBoardDetectDialogDismiss() = ClipBoardLinkDetector.clear()

    override fun onCleared() {
        super.onCleared()
        if (_playerPool.isInitialized()) {
            _playerPool.value.dispose()
        }
    }
}
