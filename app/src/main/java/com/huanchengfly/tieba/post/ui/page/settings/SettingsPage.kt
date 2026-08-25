package com.huanchengfly.tieba.post.ui.page.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.theme.BlueGrey700
import com.huanchengfly.tieba.post.theme.Cyan700
import com.huanchengfly.tieba.post.theme.Green700
import com.huanchengfly.tieba.post.theme.Purple700
import com.huanchengfly.tieba.post.theme.Red700
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.About
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AccountManage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PreferenceItemPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedTextPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

@Composable
fun SettingsPage(navigator: NavController) {
    val account = LocalAccount.current
    val context = LocalContext.current
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var keyword by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val query = keyword.trim()
    val searchResult = remember(query, context) {
        if (query.isEmpty()) {
            emptyList()
        } else {
            val q = query.lowercase()
            SettingsSearchIndex.all.filter { entry ->
                val title = context.getString(entry.titleRes).lowercase()
                val summary = entry.summaryRes?.let { context.getString(it).lowercase() }
                title.contains(q) || (summary?.contains(q) == true)
            }
        }
    }

    val closeSearch: () -> Unit = {
        searchActive = false
        keyword = ""
    }

    // 系统返回优先关闭搜索
    BackHandler(enabled = searchActive, onBack = closeSearch)

    // 搜索态展开时让列表滚回顶部, 保证搜索条停留在正确的置顶位
    LaunchedEffect(searchActive) {
        if (searchActive) listState.scrollToItem(0)
    }

    MyScaffold(
        topBar = {
            SettingsTopAppBar(
                titleRes = R.string.title_settings,
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { if (searchActive) closeSearch() else navigator.navigateUp() })
                },
                actions = {
                    if (!searchActive) {
                        ActionItem(
                            icon = Icons.Rounded.Search,
                            contentDescription = R.string.title_settings_search,
                        ) { searchActive = true }
                    }
                },
            )
        },
        snackbarHostState = rememberSnackbarHostState(),
        snackbarHost = { SwipeToDismissSnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = SettingsContentPadding,
            verticalArrangement = Arrangement.spacedBy(SettingsGroupVerticalPadding),
        ) {
            item(key = "settings_search_box") {
                AnimatedVisibility(
                    visible = searchActive,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    SettingsSearchBar(
                        keyword = keyword,
                        onKeywordChange = { keyword = it },
                    )
                }
            }
            if (searchActive && query.isNotEmpty()) {
                settingsSearchResultsList(
                    result = searchResult,
                    onOpenResult = { entry ->
                        SettingsSearchTarget.set(entry.destination, entry.itemKey)
                        navigator.navigateDebounced(entry.destination)
                    },
                )
            } else {
                item(key = "account") {
                    AccountSettingCard(
                        account = account,
                        onManageAccountClicked = {
                            navigator.navigateDebounced(route = AccountManage)
                        },
                        onLoginClicked = {
                            navigator.navigateDebounced(route = Login)
                        },
                    )
                }
                item(key = "oksign") {
                    SettingsCategoryItem(
                        title = R.string.title_oksign,
                        summary = R.string.summary_settings_oksign,
                        icon = Icons.Rounded.Checklist,
                        iconContainer = Purple700,
                        enabled = account != null,
                    ) { navigator.navigateDebounced(SettingsDestination.OKSign) }
                }
                item(key = "block") {
                    SettingsCategoryItem(
                        title = R.string.title_block_settings,
                        summary = R.string.summary_block_settings,
                        icon = Icons.Rounded.DoNotDisturbOff,
                        iconContainer = Red700,
                    ) { navigator.navigateDebounced(SettingsDestination.BlockSettings) }
                }
                item(key = "ui") {
                    SettingsCategoryItem(
                        title = R.string.title_settings_custom,
                        summary = R.string.summary_settings_custom,
                        icon = Icons.Outlined.FormatPaint,
                        iconContainer = Green700,
                    ) { navigator.navigateDebounced(SettingsDestination.UI) }
                }
                item(key = "habit") {
                    SettingsCategoryItem(
                        title = R.string.title_settings_read_habit,
                        summary = R.string.summary_settings_habit,
                        icon = Icons.Outlined.DashboardCustomize,
                        iconContainer = Green700,
                    ) { navigator.navigateDebounced(SettingsDestination.Habit) }
                }
                item(key = "privacy") {
                    SettingsCategoryItem(
                        title = R.string.title_settings_privacy,
                        summary = R.string.summary_settings_privacy,
                        icon = Icons.Outlined.Shield,
                        iconContainer = Cyan700,
                    ) { navigator.navigateDebounced(SettingsDestination.Privacy) }
                }
                item(key = "more") {
                    SettingsCategoryItem(
                        title = R.string.title_settings_more,
                        summary = R.string.summary_settings_more,
                        icon = Icons.Rounded.MoreHoriz,
                        iconContainer = BlueGrey700,
                    ) { navigator.navigateDebounced(SettingsDestination.More) }
                }
                item(key = "about") {
                    SettingsCategoryItem(
                        title = R.string.title_about,
                        summary = R.string.summary_settings_about,
                        icon = Icons.Outlined.Info,
                        iconContainer = BlueGrey700,
                    ) { navigator.navigate(About) }
                }
            }
        }
    }
}

@Composable
private fun AccountSettingCard(
    account: Account?,
    onManageAccountClicked: () -> Unit,
    onLoginClicked: () -> Unit,
) {
    if (account != null) {
        SegmentedPreference(
            title = { Text(text = stringResource(R.string.title_account_manage)) },
            summary = {
                val name = account.nickname ?: account.name
                Text(text = stringResource(R.string.summary_now_account, name))
            },
            leadingIcon = {
                Avatar(
                    data = remember { StringUtil.getAvatarUrl(account.portrait) },
                    modifier = Modifier.size(SettingsLeadingIconSize),
                )
            },
            shapes = ListItemDefaults.shapes().run { copy(shape = selectedShape) },
            contentPadding = PreferenceItemPadding,
            onClick = onManageAccountClicked,
        )
    } else {
        SettingsCategoryItem(
            title = R.string.title_account_manage,
            summary = R.string.summary_not_logged_in,
            icon = Icons.Rounded.AccountCircle,
            iconContainer = Purple700,
            onClick = onLoginClicked,
        )
    }
}

@Composable
private fun SettingsCategoryItem(
    @StringRes title: Int,
    @StringRes summary: Int,
    icon: ImageVector,
    iconContainer: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val iconColor = if (enabled) iconContainer else iconContainer.copy(0.38f)
    SegmentedPreference(
        title = { Text(text = stringResource(title)) },
        summary = { Text(text = stringResource(summary)) },
        contentPadding = PreferenceItemPadding,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(SettingsLeadingIconSize)
                    .background(color = iconColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else LocalContentColor.current,
                )
            }
        },
        shapes = ListItemDefaults.shapes().run { copy(shape = selectedShape) },
        enabled = enabled,
        onClick = onClick,
    )
}

/** The default expanded height of a [SettingsTopAppBar] */
private val SettingsAppbarExpandHeight: Dp
    @Composable @ReadOnlyComposable get() = with(LocalWindowAdaptiveInfo.current.windowSizeClass) {
        when {
            isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> TopAppBarDefaults.LargeAppBarExpandedHeight

            // isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) ->

            else -> TopAppBarDefaults.MediumAppBarExpandedHeight
        }
    }

/** Extra padding to be applied to the [SegmentedPrefsScreen] */
private val SettingsContentPadding: PaddingValues = PaddingValues(16.dp)

private val SettingsGroupVerticalPadding: Dp = 6.dp

private val SettingsLeadingIconSize: Dp = 40.dp

@Composable
fun SettingsTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(0.01f) // Nearly transparent
    )
    if (isWindowHeightCompact()) {
        TopAppBar(
            modifier = modifier,
            title = { Text(text = stringResource(id = titleRes)) },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
        )
    } else {
        CollapsingTopAppBar(
            modifier = modifier,
            title = {
                Text(
                    text = stringResource(id = titleRes),
                    modifier = Modifier.padding(start = 4.dp),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = SettingsAppbarExpandHeight,
            scrollBehavior = scrollBehavior,
            colors = colors,
        )
    }
}

@Composable
fun <T> SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    settings: Settings<T>,
    initialValue: T,
    destination: SettingsDestination? = null,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(LocalSnackbarHostState.current) },
    content: SettingsSegmentedPrefsScope<T>.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // 来自设置搜索的定位请求: 仅当目标页与该 destination 匹配时消费, 并滚动到对应条目
    val scrollToItemKey = remember(destination) {
        val match = SettingsSearchTarget.destination == destination
        if (match) {
            SettingsSearchTarget.itemKey.also { SettingsSearchTarget.clear() }
        } else {
            null
        }
    }

    MyScaffold(
        modifier = modifier,
        topBar = {
            SettingsTopAppBar(
                titleRes = titleRes,
                titleHorizontalAlignment = titleHorizontalAlignment,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        SegmentedPrefsScreen(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            settings = settings,
            initialValue = initialValue,
            contentPadding = contentPadding + SettingsContentPadding,
            scrollToItemKey = scrollToItemKey,
            content = content
        )
    }
}

@Composable
fun SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    destination: SettingsDestination? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(LocalSnackbarHostState.current) },
    content: SegmentedPrefsScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollToItemKey = remember(destination) {
        val match = SettingsSearchTarget.destination == destination
        if (match) {
            SettingsSearchTarget.itemKey.also { SettingsSearchTarget.clear() }
        } else {
            null
        }
    }

    MyScaffold(
        modifier = modifier,
        topBar = {
            SettingsTopAppBar(
                titleRes = titleRes,
                titleHorizontalAlignment = titleHorizontalAlignment,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        SegmentedTextPrefsScreen(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = contentPadding + SettingsContentPadding,
            scrollToItemKey = scrollToItemKey,
            content = content
        )
    }
}
