package com.huanchengfly.tieba.post

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog as MaterialAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.EXTRA_REDUCE_EFFECT
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.EXTRA_WELCOME_SETUP
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isHttp
import com.huanchengfly.tieba.post.components.ShortcutInitializer
import com.huanchengfly.tieba.post.components.ShortcutInitializer.Companion.TbShortcut
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.LocalPbInlineContentCache
import com.huanchengfly.tieba.post.ui.common.PbInlineContentCache.Companion.rememberPbInlineContentCache
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.MediaDisplayMode
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.AppLevelNavigationRail
import com.huanchengfly.tieba.post.ui.page.main.AppLevelRailWidth
import com.huanchengfly.tieba.post.ui.page.main.LocalMainNavState
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainNavState
import com.huanchengfly.tieba.post.ui.page.RootNavGraph
import com.huanchengfly.tieba.post.ui.page.TB_LITE_DOMAIN
import com.huanchengfly.tieba.post.ui.page.settings.theme.TranslucentThemeBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.MarkdownText
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalVideoPreviewState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberVideoPreviewState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.PreviewInfo
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> { error("No WindowAdaptiveInfo provided!") }

/**
 * 真实窗口的自适应信息, 不受双栏面板内紧凑宽度覆盖影响.
 * 用于底部导航占位等需要按窗口而非面板布局的判断.
 */
val LocalRealWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> { error("No WindowAdaptiveInfo provided!") }

val LocalHabitSettings = compositionLocalOf<HabitSettings> { error("No HabitSettings provided!") }

val LocalUISettings = compositionLocalOf { UISettings() }

/**
 * Thread ID of the thread detail page currently being rendered, `null` outside
 * thread pages. Used to attribute cached images to threads.
 */
val LocalCurrentThreadId = compositionLocalOf<Long?> { null }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {

    private var pendingAppLink by mutableStateOf<Destination?>(null)

    private var pendingDeepLink by mutableStateOf<NavDeepLinkRequest?>(null)

    /** 上一次配置中的最小窗口宽度(dp), 用于判断内外屏折叠切换 */
    private var lastSmallestWidthDp = 0

    private val viewModel: MainViewModel by viewModels()

    /** Used to control the initial welcome screen state in Macrobenchmark */
    private var welcomeScreen: Boolean? = null

    private suspend fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AccountUtil.isLoggedIn()) {
            askPermission(R.string.desc_permission_post_notifications, Manifest.permission.POST_NOTIFICATIONS, noRationale = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        viewModel.onAppLaunched()
        lastSmallestWidthDp = resources.configuration.smallestScreenWidthDp
        lifecycleScope.launch {
            ClientUtils.refreshActiveTimestamp()
            delay(2000L)
            runCatching {
                requestNotificationPermission()
            }
        }

        intent?.run {
            if (MacrobenchmarkConstant.TRACE_ENABLED) {
                extras?.getBoolean(EXTRA_REDUCE_EFFECT, false)?.let { reduceEffect ->
                    viewModel.settingsRepository.uiSettings.save { it.copy(reduceEffect = reduceEffect) }
                }
                welcomeScreen = extras?.getBoolean(EXTRA_WELCOME_SETUP, false)
            }
            ShortcutInitializer.getTbShortcut(this)?.also { onNewShortcut(it) }
            data?.normalizeScheme()?.let { pendingAppLink = appLinkToNavRoute(uri = it) }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val smallestWidth = newConfig.smallestScreenWidthDp
        val wasInnerScreen = lastSmallestWidthDp >= FoldableInnerScreenMinWidthDp
        lastSmallestWidthDp = smallestWidth

        // 「折叠到外屏时自动竖屏」: 从内屏(>=600dp)折叠到外屏(<600dp)时锁定竖屏,
        // 双列自动收起进入详情全屏; 展开回内屏恢复自由方向
        if (viewModel.uiState.value.uiSettings?.foldToPortrait != true) return
        when {
            wasInnerScreen &&
                smallestWidth < FoldableInnerScreenMinWidthDp &&
                requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }

            !wasInnerScreen &&
                smallestWidth >= FoldableInnerScreenMinWidthDp &&
                requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // Due to the privacy changes in Android 10, check Clipboard only when focused
        if (hasFocus) {
            viewModel.onCheckClipBoard()
        }
    }

    private fun onNewShortcut(shortcut: TbShortcut) {
        ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcut.id)
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            ShortcutInitializer.getTbShortcut(intent)?.also { onNewShortcut(it) }
            val uri = intent.data?.normalizeScheme() ?: return
            // Is TbLite DeepLink
            if (uri.scheme == TB_LITE_DOMAIN) {
                pendingDeepLink = NavDeepLinkRequest.Builder.fromUri(uri).build()
            } else {
                pendingAppLink = appLinkToNavRoute(uri)
            }
            if (pendingDeepLink == null && pendingAppLink == null && uri.isHttp()) {
                // TODO: Bug in Firefox custom Tab
                // TiebaWebView.launchCustomTab(this, uri)
                pendingAppLink = Destination.WebView(initialUrl = uri.toString(), customClient = false)
            }
        } else {
            super.onNewIntent(intent)
        }
    }

    @Composable
    override fun Content() {
        // val bottomSheetNavigator = rememberBottomSheetNavigator(skipPartiallyExpanded = true)
        val navController = rememberNavController(/* bottomSheetNavigator */)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val autoUpdatePrompt by viewModel.autoUpdatePrompt.collectAsStateWithLifecycle()
        val reduceMotion = uiState.uiSettings?.reduceMotion == true

        TiebaExtendedTheme(colorsExt = uiState.themeColor, reduceMotion) {
            TiebaLiteLocalProvider(
                habit = uiState.habitSettings ?: return@TiebaExtendedTheme, // Initializing ...
                uiSettings = uiState.uiSettings ?: return@TiebaExtendedTheme,
            ) {
                val setupFinished = if (welcomeScreen == null) {
                    uiState.uiSettings!!.setupFinished
                } else {
                    welcomeScreen == false // Override by Macrobenchmark
                }

                if (setupFinished) {
                    LaunchedDeepLinkEffect(navController)

                    StrongBox {
                        val preview by viewModel.previewInfoFlow.collectAsStateWithLifecycle()
                        ClipBoardDetectDialog(preview, viewModel::onClipBoardDetectDialogDismiss) {
                            val route: Destination = it.clipBoardLink.toRoute(avatarUrl = it.icon?.url)
                            navController.navigate(route = route)
                        }

                        if (uiState.autoSignRestricted) {
                            BatteryOpDialog(onOpenSettings = ::requestIgnoreBatteryOptimizations)
                        }

                        autoUpdatePrompt?.let { prompt ->
                            AutoUpdatePromptDialog(
                                prompt = prompt,
                                onDismiss = viewModel::dismissAutoUpdatePrompt,
                                onDownload = viewModel::downloadAutoUpdate,
                                onInstall = viewModel::installAutoUpdate,
                            )
                        }
                    }
                } else {
                    intent.data = null
                }

                val mainNavState = remember { MainNavState() }
                val navigateToMainTab: (MainDestination) -> Unit = { dest ->
                    mainNavState.requestedTab = dest
                    navController.navigate(Destination.Main) {
                        popUpTo<Destination.Main>()
                        launchSingleTop = true
                    }
                }
                val isCompact = isWindowWidthCompact()
                val currentRootEntry by navController.currentBackStackEntryAsState()
                val showAppLevelRail =
                    !isCompact && currentRootEntry?.destination?.hasRoute<Destination.Welcome>() != true

                CompositionLocalProvider(LocalMainNavState provides mainNavState) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = if (showAppLevelRail) AppLevelRailWidth else 0.dp)
                        ) {
                            RootNavGraph(
                                // bottomSheetNavigator = bottomSheetNavigator,
                                navController = navController,
                                reduceMotion = reduceMotion,
                                settingsRepo = viewModel.settingsRepository,
                                startDestination = if (setupFinished) Destination.Main else Destination.Welcome
                            )
                        }
                        if (showAppLevelRail) {
                            AppLevelNavigationRail(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(AppLevelRailWidth),
                                onSelect = { dest ->
                                    // 当前 tab 点击沿详情状态机往回走:
                                    // 全屏 -> 双栏 -> 列表全屏 -> 根页
                                    if (dest === mainNavState.currentTab) {
                                        when {
                                            mainNavState.paneDetailExpanded ->
                                                mainNavState.collapsePaneDetailRequest++

                                            mainNavState.paneDetailOpen ->
                                                mainNavState.closePaneDetailRequest++

                                            else -> navigateToMainTab(dest)
                                        }
                                    } else {
                                        navigateToMainTab(dest)
                                    }
                                },
                                onLoginClick = { navController.navigate(Destination.Login) },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AutoUpdatePromptDialog(
        prompt: AutoUpdatePrompt,
        onDismiss: () -> Unit,
        onDownload: () -> Unit,
        onInstall: () -> Unit,
    ) {
        val context = LocalContext.current
        MaterialAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                if (prompt.cachedFile == null) {
                    Text(stringResource(R.string.update_available_title, prompt.info.versionName))
                } else {
                    Text(stringResource(R.string.update_download_finished))
                }
            },
            text = {
                if (prompt.cachedFile == null) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        prompt.info.changelog?.let { changelog ->
                            MarkdownText(changelog)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (prompt.info.apkSize > 0L) {
                            Text(
                                stringResource(
                                    R.string.update_size,
                                    Formatter.formatShortFileSize(context, prompt.info.apkSize),
                                )
                            )
                        }
                    }
                } else {
                    Text(stringResource(R.string.update_install_prompt, prompt.info.versionName))
                }
            },
            confirmButton = {
                Button(onClick = if (prompt.cachedFile == null) onDownload else onInstall) {
                    Text(
                        stringResource(
                            if (prompt.cachedFile == null) R.string.btn_download
                            else R.string.update_install
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )
    }

    @Composable
    private fun TiebaExtendedTheme(
        colorsExt: ExtendedColorScheme,
        reduceMotion: Boolean,
        content: @Composable () -> Unit
    ) {
        val backgroundImage by viewModel.translucentThemeBackground.collectAsStateWithLifecycle()
        val motionScheme = if (!reduceMotion) MotionScheme.expressive() else MotionScheme.standard()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onCase(backgroundImage == null) { background(colorsExt.colorScheme.background) }
        ) {
            if (backgroundImage != null) {
                TranslucentThemeBackground(Modifier.matchParentSize(), file = backgroundImage)
            }
            TiebaLiteTheme(colorSchemeExt = colorsExt, motionScheme, content = content)
        }
    }

    @Composable
    private fun LaunchedDeepLinkEffect(navController: NavController) {
        LaunchedEffect(pendingDeepLink) {
            pendingDeepLink?.let {
                val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                runCatching {
                    navController.navigate(request = it, navOptions = navOptions)
                }
                .onFailure { e -> e.printStackTrace() }
                pendingDeepLink = null
            }
        }

        LaunchedEffect(pendingAppLink) {
            pendingAppLink?.let {
                navController.navigateDebounced(route = it)
                pendingAppLink = null
            }
        }
    }

    @NonRestartableComposable
    @Composable
    private fun TiebaLiteLocalProvider(
        habit: HabitSettings,
        uiSettings: UISettings,
        content: @Composable () -> Unit
    ) {
        val currentAccount by viewModel.account.collectAsStateWithLifecycle(initialValue = null)
        val videoPreviewState = if (habit.mediaDisplayMode != MediaDisplayMode.HIDE && habit.videoAutoplay) {
            rememberVideoPreviewState(viewModel.playerPool)
        } else {
            null
        }

        CompositionLocalProvider(
            LocalAccount provides currentAccount,
            LocalPbInlineContentCache provides rememberPbInlineContentCache(),
            LocalHabitSettings provides habit,
            LocalUISettings provides uiSettings,
            LocalVideoPreviewState provides videoPreviewState,
            content = content
        )
    }

    companion object {

        /** 内屏最小宽度阈值(dp): 大于等于该值视为内屏(大屏), 小于视为外屏 */
        private const val FoldableInnerScreenMinWidthDp = 600

        private fun Context.appLinkToNavRoute(uri: Uri): Destination? {
            return ClipBoardLinkDetector.parseDeepLink(uri)
                .onFailure {
                    toastShort(it.getErrorMessage())
                }
                .getOrNull()
                ?.toRoute()
        }

        @Composable
        private fun ClipBoardDetectDialog(
            preview: PreviewInfo?,
            onDismiss: () -> Unit,
            onOpen: (PreviewInfo) -> Unit
        ) {
            val dialogState = rememberDialogState()

            if (preview == null) return
            LaunchedEffect(Unit) {
                if (!dialogState.show) dialogState.show()
            }

            Dialog(
                dialogState = dialogState,
                dialogProperties = AnyPopDialogProperties(
                    direction = DirectionState.CENTER,
                    dismissOnClickOutside = false
                ),
                onDismiss = onDismiss,
                title = {
                    Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
                },
                buttons = {
                    DialogNegativeButton(text = stringResource(id = R.string.btn_close))
                    DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                        onOpen(preview)
                    }
                },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        preview.icon?.let { icon ->
                            val iconShape = MaterialTheme.shapes.extraSmall
                            if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                Avatar(data = icon.res, size = Sizes.Medium, shape = iconShape)
                            } else {
                                Avatar(data = icon.url, size = Sizes.Medium, shape = iconShape)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            preview.title?.let { title ->
                                Text(text = title, style = MaterialTheme.typography.titleMedium)
                            }
                            preview.subtitle?.let { subtitle ->
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun BatteryOpDialog(
            dialogState: DialogState = rememberDialogState(),
            onOpenSettings: () -> Unit
        ) {
            // Show dialog only once
            var dismissed by rememberSaveable { mutableStateOf(false) }
            if (dismissed) return

            LaunchedEffect(Unit) {
                delay(2000L)
                dialogState.show()
            }
            if (!dialogState.show) return

            Dialog(
                dialogState = dialogState,
                onDismiss = {
                    dismissed = true
                },
                title = { Text(text = stringResource(id = R.string.title_ignore_battery_optimization)) },
                content = {
                    Text(text = stringResource(id = R.string.tip_auto_sign))
                },
                buttons = {
                    DialogNegativeButton(text = stringResource(id = R.string.button_cancel))

                    DialogPositiveButton(
                        text = stringResource(id = R.string.btn_open_settings),
                        onClick = onOpenSettings
                    )
                }
            )
        }
    }
}
