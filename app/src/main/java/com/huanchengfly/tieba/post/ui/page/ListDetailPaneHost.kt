package com.huanchengfly.tieba.post.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isPhoneDevice
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadViewModel
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsPage
import com.huanchengfly.tieba.post.ui.page.main.LocalMainNavState
import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.Serializable

/**
 * 详情面板是否处于打开状态. 列表页面据此在详情关闭时重新消费
 * ThreadPage 写入的点赞/收藏结果, 保证双栏模式下列表即时刷新.
 */
@PublishedApi
internal val LocalDetailPaneOpen = compositionLocalOf { false }

/** 双栏模式下详情面板的起始占位路由, 无选中帖子时展示. */
@Serializable
private data object ListDetailPanePlaceholder

/**
 * 列表-详情双栏宿主, 供贴吧、信息流、通知等列表页面复用.
 *
 * 未选中帖子时列表保持全屏; 选中帖子后进入分屏(左列表右详情);
 * 详情顶栏可展开/收起为全屏. 紧凑宽度下行为与旧版一致: 列表全屏,
 * 点击帖子通过根导航整页打开.
 *
 * @param startSplit 进入时是否直接分屏(右侧显示占位), 用于「进入吧即分屏」模式
 * @param initialThread 进入时预置的帖子, 用于「从详情进入新吧保持右侧详情」模式
 * @param detailForumName 当前面板所属的吧名, 详情顶栏吧名 chip 指向同吧时关闭详情
 * @param respectLargeScreenDefaultSplit 是否遵循「大屏默认分栏」全局设置; 贴吧页有其独立开关故传 false
 *
 * 详情 NavHost 始终在同一个组合位置, 避免移动位置导致 setGraph 重建
 * 而清空嵌套返回栈, 保证折叠/展开切换时详情状态不丢.
 */
@Composable
fun ListDetailPaneHost(
    navigator: NavController,
    modifier: Modifier = Modifier,
    startSplit: Boolean = false,
    initialThread: Destination.Thread? = null,
    detailForumName: String? = null,
    respectLargeScreenDefaultSplit: Boolean = true,
    listPane: @Composable (onOpenThread: (Destination.Thread) -> Unit) -> Unit,
) {
    // 用「是否手机」而非当前窗口宽度判定分栏: 手机(含折叠屏外屏/竖屏大屏)始终整页跳转,
    // 避免宽度 ≥ 600dp 时误入分栏(占位+面板转场+往返回顶); 平板最小宽度 ≥ 600dp 才分栏.
    val isCompact = isPhoneDevice()
    val detailNavController = rememberNavController()
    val detailEntry by detailNavController.currentBackStackEntryAsState()
    // 详情面板里有内容(帖子或楼中楼)都视为「详情已显示」, 否则 SubPosts 在栈顶时返回会
    // 触发宿主 BackHandler 被禁用而穿透到根导航, 导致直接退回首页
    val isDetailShowing = detailEntry?.destination?.hasRoute<Destination.Thread>() == true ||
            detailEntry?.destination?.hasRoute<Destination.SubPosts>() == true
    var detailExpanded by rememberSaveable { mutableStateOf(false) }
    // 列表层状态(选中 tab + 各 tab 滚动位置): 全屏详情时列表移出组合, 用 holder 留存
    val listPaneHolder = rememberSaveableStateHolder()
    val uiSettings = LocalUISettings.current
    val effectiveStartSplit =
        startSplit || (respectLargeScreenDefaultSplit && uiSettings.largeScreenDefaultSplit)
    // 大屏下是否处于分屏布局: 已有选中帖子, 或设置了进入即分屏
    val showSplit = !isCompact && (isDetailShowing || effectiveStartSplit)

    // 折叠到紧凑宽度后, 面板详情全屏展示时通知 MainPage 隐藏底栏, 避免遮挡回复框
    val mainNavState = LocalMainNavState.current
    LaunchedEffect(isDetailShowing) {
        mainNavState.paneDetailOpen = isDetailShowing
    }
    // 镜像详情全屏状态, 供侧栏判断「先收起再跳转」
    LaunchedEffect(detailExpanded) {
        mainNavState.paneDetailExpanded = detailExpanded
    }
    // 侧栏点击当前 tab 时请求收起全屏, 回到双栏
    LaunchedEffect(mainNavState.collapsePaneDetailRequest) {
        if (mainNavState.collapsePaneDetailRequest > 0 && detailExpanded) {
            detailExpanded = false
        }
    }
    // 侧栏点击当前 tab 且详情分屏时请求关闭详情, 回到列表全屏
    LaunchedEffect(mainNavState.closePaneDetailRequest) {
        if (mainNavState.closePaneDetailRequest > 0 && isDetailShowing) {
            detailExpanded = false
            detailNavController.popBackStack()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            mainNavState.paneDetailOpen = false
            mainNavState.paneDetailExpanded = false
        }
    }
    // 面板详情模式下接管系统返回键: 全屏详情先收起回双栏, 分屏详情先关闭详情回列表全屏。
    // ThreadPage 只在帖子带收藏楼层标记等特殊状态时注册 BackHandler, 普通帖子会漏掉,
    // 导致返回键直接落到 MainPage 而退回首页; 这里在宿主层兜底保证按详情状态机分步返回。
    BackHandler(enabled = isDetailShowing && !isCompact) {
        if (detailExpanded) {
            detailExpanded = false
        } else {
            detailNavController.popBackStack()
        }
    }

    // 从详情进入新吧时预置当前帖子: 仅首次组合且详情尚未恢复时执行
    LaunchedEffect(Unit) {
        if (!isCompact && !isDetailShowing && initialThread != null) {
            detailNavController.navigate(initialThread) {
                popUpTo<ListDetailPanePlaceholder>()
            }
        }
    }

    // 面板内的内容按紧凑宽度排版: 避免双栏窄栏里图文仍走宽屏分支
    // (媒体只占 50% 宽), 高度类别保持真实窗口值.
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val paneAdaptiveInfo = remember(windowAdaptiveInfo) {
        WindowAdaptiveInfo(
            windowSizeClass = WindowSizeClass(0, windowAdaptiveInfo.windowSizeClass.minHeightDp),
            windowPosture = windowAdaptiveInfo.windowPosture,
        )
    }

    val openThread: (Destination.Thread) -> Unit = { thread ->
        if (isCompact) {
            // 紧凑宽度(手机/折叠外屏): 走根路由整页, 保留原有跳转动画, 不经过分屏占位
            navigator.navigateDebounced(thread)
        } else {
            // 大屏(平板/折叠内屏): 放入分屏详情面板, 保证「全屏/收起」键始终可用
            detailNavController.navigate(thread) {
                popUpTo<ListDetailPanePlaceholder>()
            }
        }
    }

    // 详情顶栏吧名 chip 的跳转: 目标吧就是当前面板时关闭详情回列表, 否则正常跳转
    val openForum: (Destination.Forum) -> Unit = { forum ->
        if (detailForumName != null && forum.forumName == detailForumName) {
            detailExpanded = false
            detailNavController.popBackStack()
        } else {
            navigator.navigateDebounced(forum)
        }
    }

    val detailPane: @Composable () -> Unit = {
        NavHost(
            navController = detailNavController,
            startDestination = ListDetailPanePlaceholder,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<ListDetailPanePlaceholder> {
                ListDetailPanePlaceholderContent()
            }
            composable<Destination.Thread>(typeMap = ThreadNavTypeMap) { backStackEntry ->
                with(backStackEntry.toRoute<Destination.Thread>()) {
                    val vm: ThreadViewModel = hiltViewModel()
                    ThreadPage(
                        threadId = threadId,
                        postId = postId,
                        extra = from,
                        navigator = navigator,
                        viewModel = vm,
                        detailPaneExpanded = detailExpanded,
                        onOpenForum = openForum,
                        onToggleDetailPane = if (isCompact) null else {
                            { detailExpanded = !detailExpanded }
                        },
                        onOpenSubPosts = { subPosts ->
                            if (!isCompact && uiSettings.subPostsInDualPane) {
                                detailNavController.navigate(subPosts.copy(isSheet = false))
                            } else {
                                navigator.navigateDebounced(subPosts)
                            }
                        },
                        onBack = {
                            if (!isCompact && detailExpanded) {
                                // 全屏详情先收起到双栏
                                detailExpanded = false
                            } else {
                                detailNavController.popBackStack()
                            }
                        },
                    )
                }
            }
            composable<Destination.SubPosts> { backStackEntry ->
                val params = backStackEntry.toRoute<Destination.SubPosts>()
                SubPostsPage(
                    params = params,
                    // 内容导航(回复/用户)走根导航, 返回则停在面板栈内
                    navigator = navigator,
                    onBack = { detailNavController.popBackStack() },
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalWindowAdaptiveInfo provides paneAdaptiveInfo,
        LocalDetailPaneOpen provides isDetailShowing,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // 列表层: 用 SaveableStateHolder 包住, 全屏详情时列表移出组合也能保留
            // 选中 tab 与各 tab 滚动位置, 回到双栏时恢复, 避免 pager 在 0 宽下跑飞
            when {
                isCompact && isDetailShowing -> Unit
                isCompact -> listPaneHolder.SaveableStateProvider(ForumListPaneKey) {
                    listPane(openThread)
                }
                !showSplit -> listPaneHolder.SaveableStateProvider(ForumListPaneKey) {
                    listPane(openThread)
                }
                isDetailShowing && detailExpanded -> Unit
                else -> {
                    // 分屏: 列表占左半
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ListPaneFraction)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                    ) {
                        listPaneHolder.SaveableStateProvider(ForumListPaneKey) {
                            listPane(openThread)
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
            // 详情层: 固定组合位置, 仅切换尺寸与对齐
            val detailModifier = when {
                isCompact && isDetailShowing -> Modifier.fillMaxSize()
                !showSplit -> Modifier.size(0.dp)
                isDetailShowing && detailExpanded -> Modifier.fillMaxSize()
                else -> {
                    // 分屏: 详情占右半
                    Modifier
                        .fillMaxWidth(1f - ListPaneFraction)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                }
            }
            Box(modifier = detailModifier) {
                detailPane()
            }
        }
    }
}

/** 列表层在 SaveableStateHolder 中使用的 key, 仅需与同宿主内其它条目区分. */
private const val ForumListPaneKey = "ListDetailPaneForumList"

@Composable
private fun ListDetailPanePlaceholderContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.tip_select_thread_to_view_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
/** 分屏时列表栏占窗口宽度比例. */
private const val ListPaneFraction = 0.45f
