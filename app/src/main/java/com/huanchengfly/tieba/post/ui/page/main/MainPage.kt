package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalShortNavigationBarOverride
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.window.embedding.SplitAttributes.LayoutDirection
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.LocalRealWindowAdaptiveInfo
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.defaultVerticalEnterTransition
import com.huanchengfly.tieba.post.ui.common.defaultVerticalExitTransition
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.theme.compose.withNonNull
import com.huanchengfly.tieba.post.ui.models.settings.NavigationLabel
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationPosition
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationSuiteScaffoldLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TallNavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.isNavigationBar
import com.huanchengfly.tieba.post.ui.widgets.compose.navigationSuiteScaffoldConsumeWindowInsets
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberTbHazeState
import com.huanchengfly.tieba.post.utils.DeviceUtils.vibrateOneShot
import com.huanchengfly.tieba.post.utils.LocalAccount
import dev.chrisbanes.haze.hazeSource
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sign

@Stable
val MainDestination.titleRes: Int
    @StringRes get() = when(this) {
        MainDestination.Home -> R.string.title_main
        MainDestination.Explore -> R.string.title_explore
        MainDestination.Notification -> R.string.title_notifications
        MainDestination.User -> R.string.title_user
    }

@Stable
val MainDestination.iconRes: Int
    @DrawableRes get() = when(this) {
        MainDestination.Home -> R.drawable.ic_animated_rounded_inventory_2
        MainDestination.Explore -> R.drawable.ic_animated_toy_fans
        MainDestination.Notification -> R.drawable.ic_animated_rounded_notifications
        MainDestination.User -> R.drawable.ic_animated_rounded_person
    }

val bottomNavigationPlaceholder: @Composable () -> Unit = {
    // 使用真实窗口信息: 面板内 LocalWindowAdaptiveInfo 被覆盖为紧凑宽度,
    // 会导致内屏上误判为底部导航而预留过高占位
    val navigationSuiteType = calculateMainNavigationSuiteType(LocalRealWindowAdaptiveInfo.current)
    if (navigationSuiteType.isNavigationBar) {
        Spacer(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(
                    when (navigationSuiteType) {
                        MainNavigationSuiteType.ShortNavigationBarCompact -> NavigationBarHeight
                        MainNavigationSuiteType.FloatingNavigationBar -> {
                            TallNavigationBarHeight + floatingNavigationBarCompactScreenOffset
                        }

                        MainNavigationSuiteType.FloatingNavigationBarCompact -> {
                            NavigationBarHeight + floatingNavigationBarCompactScreenOffset
                        }

                        else -> TallNavigationBarHeight
                    }
                )
        )
    }
    // 内屏沉浸式: 不预留底部占位, 页面背景延伸至手势条区域
}

/**
 * Gets the current navigation [MainDestination] as a [MutableState]. When the given navController
 * changes the back stack due to a [NavController.navigate] or [NavController.popBackStack] this
 * will trigger a recompose and return the top destination on the back stack.
 *
 * @return a mutable state of the current [MainDestination]
 */
@Composable
private fun NavController.currentMainDestinationAsState(destinations: List<MainDestination>): State<MainDestination?> {
    val lifecycle = LocalLifecycleOwner.current
    return produceState(initialValue = null, destinations, currentBackStackEntryFlow, lifecycle) {
        withContext(Dispatchers.Default) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                currentBackStackEntryFlow.collect {
                    value = destinations.fastFirstOrNull { dest -> it.destination.hasRoute(dest::class) }
                }
            }
        }
    }
}

@Composable
fun MainPage(
    navHostController: NavHostController,
    startDestination: MainDestination = MainDestination.Home,
    vm: MainPageViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val nestedNavController = rememberNavController()
    val scaffoldState = rememberNavigationSuiteScaffoldState()
    val uiSettings = LocalUISettings.current
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val mainNavState = LocalMainNavState.current
    // 非紧凑窗口下主导航由应用级常驻侧栏承担, MainPage 自身不再渲染导航套件
    val navigationSuiteType = when {
        !isWindowWidthCompact() -> MainNavigationSuiteType.None
        mainNavState.paneDetailOpen -> MainNavigationSuiteType.None
        else -> calculateMainNavigationSuiteType()
    }

    val loggedIn = LocalAccount.current != null
    val destinations = remember(loggedIn, uiSettings.hideExplore) {
        listOfNotNull(
            MainDestination.Home,
            MainDestination.Explore.takeUnless { uiSettings.hideExplore },
            MainDestination.Notification.takeIf { loggedIn },
            MainDestination.User,
        )
    }

    val blurEffect = !uiSettings.reduceEffect && !MaterialTheme.colorScheme.isTranslucent
    val hazeState = if (blurEffect) rememberTbHazeState() else null
    val navigationSuiteColors = mainNavigationSuiteColors(uiSettings.bottomNavFloating, blurEffect)

    val currentDestinationState = nestedNavController.currentMainDestinationAsState(destinations)
    val currentDestination by currentDestinationState

    // 侧栏点击: 切换嵌套导航到目标 tab
    LaunchedEffect(mainNavState.requestedTab) {
        mainNavState.requestedTab?.let { dest ->
            mainNavState.requestedTab = null
            nestedNavController.navigate(route = dest) {
                launchSingleTop = true
                restoreState = true
                popUpTo(route = startDestination) {
                    saveState = true
                }
            }
            if (dest == MainDestination.Notification) vm.onNavigateNotification()
        }
    }

    // 向侧栏上报当前 tab, 用于高亮
    LaunchedEffect(currentDestination) {
        mainNavState.currentTab = currentDestination
    }

    val scrollHideEnabled = navigationSuiteType.isFloatingNavigationBar && uiSettings.bottomNavHideOnScroll
    val scrollHideThreshold = with(LocalDensity.current) { 24.dp.toPx() }
    val bottomNavScrollConnection = remember(scrollHideEnabled, scrollHideThreshold) {
        if (!scrollHideEnabled) null
        else object : NestedScrollConnection {
            private var scrollDelta = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                // 只在动态页参与滑动隐藏, 其余 tab 底栏常驻
                if (currentDestinationState.value != MainDestination.Explore) return Offset.Zero

                // 只累计子级真正消费的滚动量，避免内容不足一屏时的拖拽手势触发隐藏
                val dy = consumed.y
                if (dy == 0f) return Offset.Zero
                if (scrollDelta.sign != dy.sign) scrollDelta = 0f
                scrollDelta += dy

                if (scrollDelta <= -scrollHideThreshold &&
                    scaffoldState.targetValue != NavigationSuiteScaffoldValue.Hidden
                ) {
                    coroutineScope.launch { scaffoldState.hide() }
                    scrollDelta = 0f
                } else if (scrollDelta >= scrollHideThreshold &&
                    scaffoldState.targetValue != NavigationSuiteScaffoldValue.Visible
                ) {
                    coroutineScope.launch { scaffoldState.show() }
                    scrollDelta = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(scrollHideEnabled, currentDestination) {
        if ((!scrollHideEnabled || currentDestination != MainDestination.Explore) &&
            scaffoldState.targetValue != NavigationSuiteScaffoldValue.Visible
        ) {
            scaffoldState.show()
        }
    }
    // 回顶操作（回顶 FAB、重复点击底栏等）时确保底栏重新弹出
    onGlobalEvent<GlobalEvent.ScrollToTop>(coroutineScope) {
        if (scaffoldState.targetValue != NavigationSuiteScaffoldValue.Visible) {
            coroutineScope.launch { scaffoldState.show() }
        }
    }
    MainNavigationSuiteScaffold(
        state = scaffoldState,
        hazeState = hazeState.takeIf { navigationSuiteType.isNavigationBar },
        scrollConnection = bottomNavScrollConnection,
        navigationItemCount = destinations.size,
        navigationItems = {
            val messageCount by vm.messageCountFlow.collectAsStateWithLifecycle()

            MainNavigationItems(
                items = destinations,
                isSelected = { dest -> dest === currentDestination },
                onSelect = { dest ->
                    if (scaffoldState.targetValue != NavigationSuiteScaffoldValue.Visible) {
                        coroutineScope.launch { scaffoldState.show() }
                    }
                    nestedNavController.navigate(route = dest) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(route = startDestination) {
                            saveState = true
                        }
                    }
                    if (dest == MainDestination.Notification) vm.onNavigateNotification()
                },
                onReSelect = { dest ->
                    if (scaffoldState.targetValue != NavigationSuiteScaffoldValue.Visible) {
                        coroutineScope.launch { scaffoldState.show() }
                    }
                    coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(dest))
                },
                mainNavigationSuiteType = navigationSuiteType,
                bottomNavLabel = uiSettings.bottomNavLabel,
                messageCount = { messageCount },
            )
        },
        mainNavSuiteType = navigationSuiteType,
        navigationSuiteColors = navigationSuiteColors,
        navigationVerticalArrangement = calculateNavigationPosition(windowAdaptiveInfo),
        primaryActionContent = {
            val onLoginClicked: () -> Unit = { navHostController.navigate(Destination.Login) }
            when (navigationSuiteType) {
                MainNavigationSuiteType.WideNavigationRailCollapsed,
                MainNavigationSuiteType.WideNavigationRailExpanded -> {
                    AccountNavIcon(onLoginClicked, modifier = Modifier.padding(start = 32.dp))
                }
                MainNavigationSuiteType.NavigationRail -> {
                    AccountNavIcon(onLoginClicked, modifier = Modifier.padding(top = 10.dp))
                }
                MainNavigationSuiteType.NavigationDrawer -> TbDrawerNavigationAction(onLoginClicked)

                MainNavigationSuiteType.FloatingNavigationBarCompact -> {
                    if (uiSettings.hideExplore) return@MainNavigationSuiteScaffold
                    ExplorePrimaryAction(visible = MainDestination.Explore === currentDestination) {
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(MainDestination.Explore))
                    }
                }

                else -> Unit // NavigationBar or None
            }
        }
    ) {
        val parentAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val parentSharedTransitionScope = LocalSharedTransitionScope.current
        val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            mainEnterTransition(navigationSuiteType, destinations)
        }
        val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            mainExitTransition(navigationSuiteType, destinations)
        }

        NavHost(
            navController = nestedNavController,
            startDestination = startDestination,
            modifier = Modifier.onNotNull(hazeState) {
                hazeSource(state = it.state, zIndex = 1f)
            },
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = enterTransition,
            popExitTransition = exitTransition
        ) {
            mainNavGraph(
                navController = navHostController,
                nestedNavController = nestedNavController,
                hazeState = hazeState,
                parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
                parentSharedTransitionScope = parentSharedTransitionScope,
            )
        }
    }

    currentDestination?.let {
        BackHandler(it !== startDestination) {
            nestedNavController.popBackStack(route = startDestination::class, inclusive = false, saveState = true)
        }
    }
}

@NonRestartableComposable
@Composable
private fun MainNavigationSuite(
    mainNavigationSuiteType: MainNavigationSuiteType,
    modifier: Modifier = Modifier,
    navigationItemCount: Int = MaxFloatingNavigationItems,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    val shortNavBarOverride = when (mainNavigationSuiteType) {
        MainNavigationSuiteType.FloatingNavigationBar -> FloatingNavigationBarOverride
        MainNavigationSuiteType.FloatingNavigationBarCompact -> FloatingIconNavigationBarOverride
        MainNavigationSuiteType.NavigationBar -> DefaultNavigationBarOverride
        else -> androidx.compose.material3.DefaultShortNavigationBarOverride
    }
    CompositionLocalProvider(
        LocalShortNavigationBarOverride provides shortNavBarOverride,
        LocalMainNavigationItemCount provides navigationItemCount,
    ) {
        NavigationSuite(
            navigationSuiteType = mainNavigationSuiteType.toNavigationSuiteType(),
            modifier = modifier,
            colors = colors,
            verticalArrangement = verticalArrangement,
            primaryActionContent = primaryActionContent,
            content = content,
        )
    }
}

/**
 * [NavigationSuiteScaffold] with Haze blur support
 *
 * @see NavigationSuiteScaffoldLayout
 * @see Modifier.navigationSuiteScaffoldConsumeWindowInsets
 * */
@Composable
private fun MainNavigationSuiteScaffold(
    navigationItems: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationItemCount: Int = MaxFloatingNavigationItems,
    hazeState: TbHazeState? = null,
    mainNavSuiteType: MainNavigationSuiteType = calculateMainNavigationSuiteType(),
    navigationBarAtop: Boolean = true,
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    navigationVerticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    scrollConnection: NestedScrollConnection? = null,
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit = {},
) {
    val navigationSuiteType = mainNavSuiteType.toNavigationSuiteType()
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    // Override navbar container color when there is ongoing transition
    val colorsOnTransition = if (navigationSuiteType.isNavigationBar && animatedVisibilityScope != null) {
        animatedVisibilityScope.navigationSuiteTransitionColors(mainNavSuiteType, navigationSuiteColors).value
    } else {
        null
    }

    NavigationSuiteScaffoldLayout(
        modifier = modifier,
        navigationSuite = {
            MainNavigationSuite(
                mainNavigationSuiteType = mainNavSuiteType,
                navigationItemCount = navigationItemCount,
                modifier = Modifier
                    .withNonNull(hazeState) {
                        Modifier.defaultHazeEffect {
                            blurEnabled = animatedVisibilityScope?.transition?.isRunning != true
                        }
                    }
                    .onNotNull(colorsOnTransition) {
                        animateEnterExit(
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedTransitionScope = LocalSharedTransitionScope.current,
                            enter = defaultVerticalEnterTransition(topToBottom = false),
                            exit = defaultVerticalExitTransition(topToBottom = false)
                        )
                    },
                colors = colorsOnTransition ?: navigationSuiteColors,
                verticalArrangement = navigationVerticalArrangement,
                primaryActionContent = primaryActionContent,
                content = navigationItems,
            )
        },
        state = state,
        navigationSuiteType = navigationSuiteType,
        navigationBarAtop = navigationBarAtop,
        bottomOffset = if (mainNavSuiteType.isFloatingNavigationBar) {
            floatingNavigationBarCompactScreenOffset
        } else {
            0.dp
        },
        primaryActionContent = primaryActionContent,
        primaryActionContentHorizontalAlignment = primaryActionContentHorizontalAlignment,
        content = {
            Box(
                Modifier
                    .onNotNull(scrollConnection) { nestedScroll(it) }
                    .navigationSuiteScaffoldConsumeWindowInsets(
                        navigationSuiteType,
                        navigationBarAtop,
                        state,
                    ),
            ) {
                content()
            }
        },
    )
}

internal const val MaxFloatingNavigationItems = 4
internal val LocalMainNavigationItemCount = compositionLocalOf { MaxFloatingNavigationItems }

@Stable
private fun NavigationLabel.visible(selected: Boolean): Boolean {
    return when(this) {
        NavigationLabel.ALWAYS -> true
        NavigationLabel.SELECTED -> selected
        NavigationLabel.NONE -> false
    }
}

@Composable
private fun MainNavigationItems(
    items: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    modifier: Modifier = Modifier,
    onSelect: (MainDestination) -> Unit = {},
    onReSelect: (MainDestination) -> Unit = {},
    mainNavigationSuiteType: MainNavigationSuiteType = calculateMainNavigationSuiteType(),
    bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    messageCount: () -> String? = { null },
) {
    val isNavigationBar = mainNavigationSuiteType.isNavigationBar
    items.fastForEach { destination ->
        key(destination) {
            val selected = isSelected(destination)
            MainNavigationSuiteItem(
                selected = selected,
                onClick = {
                    if (selected) onReSelect(destination) else onSelect(destination)
                },
                icon = {
                    Icon(
                        painter = rememberAnimatedVectorPainter(
                            animatedImageVector = AnimatedImageVector.animatedVectorResource(destination.iconRes),
                            atEnd = selected
                        ),
                        modifier = Modifier.size(Sizes.Tiny),
                        contentDescription = stringResource(destination.titleRes),
                    )
                },
            label = if (mainNavigationSuiteType != MainNavigationSuiteType.NavigationRail &&
                (!isNavigationBar || bottomNavLabel.visible(selected))
            ) {
                { Text(stringResource(id = destination.titleRes)) }
            } else {
                null
            },
            modifier = modifier
                .onCase(mainNavigationSuiteType == MainNavigationSuiteType.NavigationDrawer) {
                    padding(horizontal = 16.dp)
                },
            mainNavigationSuiteType = mainNavigationSuiteType,
            badge = if (destination === MainDestination.Notification) {
                {
                    messageCount()?.let { messageCountText ->
                        Badge {
                            Text(
                                text = messageCountText, // 6.sp ~ BadgeTokens.LargeLabelTextFont.fontSize
                                autoSize = TextAutoSize.StepBased(6.sp, LocalTextStyle.current.fontSize),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else null,
            )
        }
    }
}

// Override NavigationDrawer to use our old custom NavigationDrawerItem
@Composable
private fun MainNavigationSuiteItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    mainNavigationSuiteType: MainNavigationSuiteType,
    enabled: Boolean = true,
    badge: @Composable (() -> Unit)? = null,
    colors: NavigationItemColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    if (mainNavigationSuiteType == MainNavigationSuiteType.FloatingNavigationBarCompact) {
        IconNavigationItem(
            selected = selected,
            onClick = onClick,
            icon = {
                if (badge != null) {
                    BadgedBox(badge = { badge.invoke() }, content = { icon() })
                } else {
                    icon()
                }
            },
            colors = colors ?: ShortNavigationBarItemDefaults.colors(),
            enabled = enabled,
            modifier = modifier,
            interactionSource = interactionSource,
        )
    } else if (mainNavigationSuiteType != MainNavigationSuiteType.NavigationDrawer) {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            modifier = modifier,
            navigationSuiteType = mainNavigationSuiteType.toNavigationSuiteType(),
            enabled = enabled,
            badge = badge,
            colors = colors,
            interactionSource = interactionSource,
        )
    } else {
        val actualColors = if (colors != null) {
            NavigationDrawerItemDefaults.colors(
                selectedIconColor = colors.selectedIconColor,
                selectedTextColor = colors.selectedTextColor,
                unselectedIconColor = colors.unselectedIconColor,
                unselectedTextColor = colors.unselectedTextColor,
                selectedContainerColor = colors.selectedIndicatorColor,
            )
        } else {
            NavigationSuiteDefaults.itemColors().navigationDrawerItemColors
        }

        NavigationDrawerItem(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            icon = icon,
            badge = badge,
            label = { label?.invoke() ?: Text("") },
            colors = actualColors,
            interactionSource = interactionSource,
        )
    }
}

/**
 * @return [NavigationSuiteColors] for background blurring
 *  */
@Composable
private fun mainNavigationSuiteColors(floatingNavBar: Boolean, blur: Boolean): NavigationSuiteColors {
    return TiebaLiteTheme.extendedColorScheme.run {
        NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarColor.copy(
                    alpha = when {
                        colorScheme.isTranslucent -> 0.65f
                        blur -> if (darkTheme) 0.86f else 0.74f
                        else -> 1f
                    }
                )
            } else {
                navigationContainer
            },
            shortNavigationBarContentColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarContentColor
            } else {
                ShortNavigationBarDefaults.contentColor
            },
            navigationBarContainerColor = navigationContainer
        )
    }
}

// Override navigation bar container color when there is ongoing transition
@Composable
private fun AnimatedVisibilityScope.navigationSuiteTransitionColors(
    navigationSuiteType: MainNavigationSuiteType,
    defaultColors: NavigationSuiteColors
): State<NavigationSuiteColors> {
    // Replace with NavigationSuiteColors.copy()!!
    val colorsInTransition = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = MaterialTheme.colorScheme.surface,
        shortNavigationBarContainerColor = if (navigationSuiteType.isFloatingNavigationBar) {
            MaterialTheme.colorScheme.vibrantFloatingNavigationBarColor
        } else {
            MaterialTheme.colorScheme.surface
        },
    )
    return remember(defaultColors) {
        derivedStateOf { if (transition.isRunning) colorsInTransition else defaultColors }
    }
}

@Composable
private fun ExplorePrimaryAction(modifier: Modifier = Modifier, visible: Boolean, onClick: () -> Unit) {
    val isTransitionActive = LocalAnimatedVisibilityScope.current?.transition?.isRunning == true
    val screenOffset = floatingNavigationBarCompactScreenOffset
    val visibilityAnimation by animateFloatAsState(
        targetValue = if (visible && !isTransitionActive) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    DefaultBackToTopFAB(
        modifier = modifier
            .graphicsLayer { // SlideIn vertically
                translationY = lerp(size.height * 2, -screenOffset.toPx(), visibilityAnimation)
            },
        visible = visible,
        size = NavigationBarHeight,
        onClick = onClick,
    )
}

// Common ScrollToTop event listener for main destinations
@Composable
inline fun <reified T: MainDestination> OnMainNavigationScrollTopEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    topAppBarState: TopAppBarState? = null,
    gridState: LazyGridState? = null,
    crossinline listState: () -> LazyListState?,
) {
    val context = LocalContext.current
    onGlobalEvent<GlobalEvent.ScrollToTop>(coroutineScope, filter = { it.tag is T }) {
        val listState = listState()
        if (listState?.canScrollBackward == true || gridState?.canScrollBackward == true) {
            context.vibrateOneShot(milliseconds = 50)
            coroutineScope.launch {
                listState?.run {
                    if (firstVisibleItemIndex > 5) scrollToItem(0) else animateScrollToItem(0)
                }
                gridState?.animateScrollToItem(0)
            }
        }
        // Reset TopAppBarState
        topAppBarState?.contentOffset = 0f
        topAppBarState?.heightOffset = 0f
    }
}

/**
 * Creates a list of [TopAppBarState] that is remembered across compositions.
 * */
@Composable
fun rememberTopAppBarScrollBehaviors(
    size: Int,
    init: @Composable (TopAppBarState) -> TopAppBarScrollBehavior
): List<TopAppBarScrollBehavior> {
    var result: List<TopAppBarScrollBehavior> by remember(size) { mutableStateOf(emptyList()) }

    val stateList = rememberSaveable(size, saver = Saver) {
        val states = mutableListOf<TopAppBarState>()
        repeat(size) {
            states.add(TopAppBarState(-Float.MAX_VALUE, 0f, 0f))
        }
        states.toImmutableList()
    }

    if (result.isEmpty()) {
        result = stateList.fastMap { init(it) }
    }
    return result
}

/** The default [Saver] implementation for list of [TopAppBarState]. */
@Suppress("UNCHECKED_CAST")
private val Saver: Saver<List<TopAppBarState>, *> = listSaver(
    save = {
        it.mapIndexed { i, it ->
            mutableListOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset)
        }.reduce { rec, list ->
            rec.apply { addAll(list) }
        }
    },
    restore = {
        assert(it.isNotEmpty())
        val states = mutableListOf<TopAppBarState>()
        var trimList = it
        val saver = (TopAppBarState.Saver as Saver<TopAppBarState, List<Float>>)
        repeat(it.size / 3) { i ->
            trimList = it.subList(i * 3, it.size)
            val state = saver.restore(trimList)!!
            states.add(state)
        }
        states.toImmutableList()
    }
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTransitionDirection(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): LayoutDirection? {
    var from = -1
    var to = -1
    items.fastForEachIndexed { i, dest ->
        if (from == -1 && initialState.destination.hasRoute(dest::class)) {
            from = i
        } else if (to == -1 && targetState.destination.hasRoute(dest::class)) {
            to = i
        }
    }
    return when {
        from == -1 || to == -1 -> null // Edge case: initialize

        navigationSuiteType.isNavigationBar ->
            if (from > to) LayoutDirection.RIGHT_TO_LEFT else LayoutDirection.LEFT_TO_RIGHT

        navigationSuiteType == MainNavigationSuiteType.None -> null

        else -> if (from > to) LayoutDirection.BOTTOM_TO_TOP else LayoutDirection.TOP_TO_BOTTOM
    }
}

// Pager style enter transition
private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainEnterTransition(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): EnterTransition {
    return when(val direction = mainTransitionDirection(navigationSuiteType, items)) {
        LayoutDirection.RIGHT_TO_LEFT, LayoutDirection.LEFT_TO_RIGHT -> {
            slideInHorizontally(
                animationSpec = MAIN_TRANSITION_SPEC,
                initialOffsetX = { if (direction == LayoutDirection.LEFT_TO_RIGHT) it else -it }
            ) + MAIN_FADE_IN_TRANSITION
        }
        LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
            slideInVertically(
                animationSpec = MAIN_TRANSITION_SPEC,
                initialOffsetY = { if (direction == LayoutDirection.TOP_TO_BOTTOM) it else -it }
            ) + MAIN_FADE_IN_TRANSITION
        }
        else -> MAIN_FADE_IN_TRANSITION
    }
}

// Pager style exit transition
private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainExitTransition(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): ExitTransition {
    return when(val direction = mainTransitionDirection(navigationSuiteType, items)) {
        LayoutDirection.RIGHT_TO_LEFT, LayoutDirection.LEFT_TO_RIGHT -> {
            slideOutHorizontally(
                animationSpec = MAIN_TRANSITION_SPEC,
                targetOffsetX = { if (direction == LayoutDirection.LEFT_TO_RIGHT) -it else it }
            ) + MAIN_FADE_OUT_TRANSITION
        }
        LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
            slideOutVertically(
                animationSpec = MAIN_TRANSITION_SPEC,
                targetOffsetY = { if (direction == LayoutDirection.TOP_TO_BOTTOM) -it else it }
            ) + MAIN_FADE_OUT_TRANSITION
        }
        else -> MAIN_FADE_OUT_TRANSITION
    }
}

private val MAIN_TRANSITION_SPEC: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = 250, easing = FastOutSlowInEasing)

private val MAIN_FADE_IN_TRANSITION: EnterTransition =
    fadeIn(tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val MAIN_FADE_OUT_TRANSITION: ExitTransition =
    fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing))

@ReadOnlyComposable
@Composable
fun calculateMainNavigationSuiteType(): MainNavigationSuiteType {
    val uiSettings = LocalUISettings.current
    return MainNavigationSuiteType.fromNavigationSuiteType(
        type = calculateNavigationType(LocalWindowAdaptiveInfo.current),
        floating = uiSettings.bottomNavFloating,
        noLabel = uiSettings.bottomNavLabel == NavigationLabel.NONE
    )
}

/** 使用指定的窗口自适应信息计算主导航形态, 供面板内按真实窗口判断时使用. */
@ReadOnlyComposable
@Composable
fun calculateMainNavigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): MainNavigationSuiteType {
    val uiSettings = LocalUISettings.current
    return MainNavigationSuiteType.fromNavigationSuiteType(
        type = calculateNavigationType(adaptiveInfo),
        floating = uiSettings.bottomNavFloating,
        noLabel = uiSettings.bottomNavLabel == NavigationLabel.NONE
    )
}

private val MainNavigationSuiteType.isNavigationBar
    get() = when (this) {
        MainNavigationSuiteType.FloatingNavigationBar,
        MainNavigationSuiteType.FloatingNavigationBarCompact,
        MainNavigationSuiteType.ShortNavigationBarCompact,
        MainNavigationSuiteType.ShortNavigationBarMedium,
        MainNavigationSuiteType.NavigationBar -> true
        else -> false
    }

@Stable
private fun MainNavigationSuiteType.toNavigationSuiteType(): NavigationSuiteType = when (this) {
    MainNavigationSuiteType.FloatingNavigationBar,
    MainNavigationSuiteType.FloatingNavigationBarCompact,
    MainNavigationSuiteType.ShortNavigationBarCompact -> NavigationSuiteType.ShortNavigationBarCompact
    MainNavigationSuiteType.ShortNavigationBarMedium -> NavigationSuiteType.ShortNavigationBarMedium
    MainNavigationSuiteType.WideNavigationRailCollapsed -> NavigationSuiteType.WideNavigationRailCollapsed
    MainNavigationSuiteType.WideNavigationRailExpanded -> NavigationSuiteType.WideNavigationRailExpanded
    MainNavigationSuiteType.NavigationBar -> NavigationSuiteType.NavigationBar
    MainNavigationSuiteType.NavigationRail -> NavigationSuiteType.NavigationRail
    MainNavigationSuiteType.NavigationDrawer -> NavigationSuiteType.NavigationDrawer
    MainNavigationSuiteType.None -> NavigationSuiteType.None
}

@Preview("MainNavigationItems", device = Devices.PIXEL_TABLET)
@Composable
private fun MainNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            MainNavigationSuiteType.WideNavigationRailCollapsed,
            MainNavigationSuiteType.WideNavigationRailExpanded,
            MainNavigationSuiteType.NavigationRail,
            MainNavigationSuiteType.NavigationDrawer,
        )
        .forEach { type ->
            MainNavigationSuite(mainNavigationSuiteType = type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}

@Preview("MainBottomNavigationItems", device = Devices.PIXEL_9)
@Composable
private fun MainBottomNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            MainNavigationSuiteType.FloatingNavigationBar,
            MainNavigationSuiteType.FloatingNavigationBarCompact,
            MainNavigationSuiteType.ShortNavigationBarCompact,
            MainNavigationSuiteType.ShortNavigationBarMedium,
            MainNavigationSuiteType.NavigationBar,
        )
        .forEach { type ->
            MainNavigationSuite(mainNavigationSuiteType = type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}
