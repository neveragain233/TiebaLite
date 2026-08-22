package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material.icons.rounded.FaceRetouchingOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.NoWindowInsets
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isFullyCollapsed
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.defaultVerticalEnterTransition
import com.huanchengfly.tieba.post.ui.common.defaultVerticalExitTransition
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.theme.compose.withNonNull
import com.huanchengfly.tieba.post.ui.models.settings.FullscreenButtonStyle
import com.huanchengfly.tieba.post.ui.models.settings.CompactReplyBarPosition
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.setResult
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CardHorizontalSpacing
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.collapsedFraction
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.fixedTopBarPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.scrollToItemWithHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.useStickyHeaderWorkaround
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.trace
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val ThreadToolbarContainerHeight = 48.dp

/**
 * Offset from the edge of the screen used for [ThreadFloatingToolbar].
 * */
private val ThreadToolbarScreenOffset = FloatingToolbarDefaults.ScreenOffset / 2

/** 评论导航接近末尾多少楼时提前预加载下一页, 避免临界点再加载导致来回跳动. */
private const val NavPreloadNearEnd = 3

const val ThreadResultKey = "THREAD_PAGE"

private fun createResult(threadId: Long, like: Like?, markedPostId: Long?): ThreadResult? {
    return if (like != null) {
        ThreadResult(threadId, liked = like.liked, likes = like.count, markedPostId = markedPostId)
    } else {
        null
    }
}

/** 评论导航在触发加载后, 待列表更新再定位的目标. */
private data class PendingCommentNav(
    val direction: CommentNavDirection,
    val anchorPostId: Long,
)

@Composable
private fun ToggleButton(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (checked) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
        contentColor = if (checked) colorScheme.onSecondaryContainer else colorScheme.onSurface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = icon, contentDescription = text)
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun LazyListState.middleVisiblePost(uiState: ThreadUiState): PostData? = layoutInfo.run {
    var postItem = visibleItemsInfo.getOrNull(visibleItemsInfo.size / 2)
    if (postItem == null || postItem.contentType !== Type.Post) {
        // Not found, search last visible post
        postItem = visibleItemsInfo.lastOrNull { it.contentType === Type.Post } ?: return uiState.firstPost
    }
    // item key is Post ID
    val postId = postItem.key as Long
    return uiState.data.fastFirstOrNull { p -> p.id == postId } ?: uiState.firstPost
}

/**
 * 评论导航的锚点: 取视口顶部第一个可见楼层, 使「上一楼/下一楼」按可视顺序逐层跳转.
 *
 * 与 [middleVisiblePost](取中间楼, 用于收藏楼层/返回键) 区分, 避免锚点漂移导致方向感知错乱.
 */
private fun LazyListState.navigationAnchorPost(uiState: ThreadUiState): PostData? = layoutInfo.run {
    // 楼主帖(FirstPost)也视为可锚定帖子: 进入详情后首个可见帖是楼主帖, 这样第一按 ▼ 落到 2 楼
    // 优先取「顶边已进入视口」的首个帖子; 跳转让位后, 上一楼顶部在视口外, 不会被误选为锚点
    val viewportStart = layoutInfo.viewportStartOffset
    val postItem = visibleItemsInfo.firstOrNull {
        (it.contentType === Type.Post || it.contentType === Type.FirstPost) &&
                (it.offset - viewportStart) >= 0
    } ?: visibleItemsInfo.firstOrNull {
        it.contentType === Type.Post || it.contentType === Type.FirstPost
    }
        ?: return uiState.firstPost
    if (postItem.contentType === Type.FirstPost) {
        return uiState.firstPost
    }
    // item key is Post ID
    val postId = postItem.key as Long
    return uiState.data.fastFirstOrNull { p -> p.id == postId } ?: uiState.firstPost
}

@Composable
fun ThreadPage(
    threadId: Long,
    postId: Long = 0,
    extra: ThreadFrom? = null,
    navigator: NavController,
    viewModel: ThreadViewModel,
    onBack: (() -> Unit)? = null,
    detailPaneExpanded: Boolean = false,
    onToggleDetailPane: (() -> Unit)? = null,
    onOpenForum: ((Destination.Forum) -> Unit)? = null,
) = trace(MacrobenchmarkConstant.TRACE_THREAD) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    val useStickyHeader = LocalHabitSettings.current.stickyHeader
    val useStickyHeaderWorkaround = useStickyHeaderWorkaround()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmpty by remember {
        derivedStateOf { state.data.isEmpty() && state.firstPost == null }
    }

    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = if (useStickyHeader) {
        TopAppBarDefaults.pinnedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

    val layout = remember(state) { buildThreadListLayout(state) }
    var pendingCommentNav by remember { mutableStateOf<PendingCommentNav?>(null) }
    // 上次导航跳转到的楼层, 作为下一次导航的锚点, 保证连续按 ▲▼ 逐楼前进
    var lastNavAnchorPostId by remember { mutableStateOf<Long?>(null) }
    // 导航键自身触发的滚动进行中; 期间不隐藏导航键
    var navScrollActive by remember { mutableStateOf(false) }
    // 评论导航坞与回复栏一致: 内容向下滚动时隐藏, 直到向上滚动才恢复
    val commentNavHidden by remember {
        derivedStateOf { toolbarScrollBehavior.state.collapsedFraction >= 0.9f }
    }
    val hideCommentNav = commentNavHidden && !navScrollActive
    // 导航键触发程序化滚动后, 复位回复栏收起状态, 保证连续导航不被隐藏
    fun resetCommentNavDock() {
        toolbarScrollBehavior.state.contentOffset = 0f
        toolbarScrollBehavior.state.offset = 0f
    }
    val useStickyThreadHeader = useStickyHeader && !useStickyHeaderWorkaround
    // 顶栏(含状态栏)高度: 跳转时让出, 避免目标楼层头像/昵称被顶栏裁掉
    val density = LocalDensity.current
    val topBarInsetPx = WindowInsets.statusBars.getTop(density) +
            with(density) { TopAppBarDefaults.TopAppBarExpandedHeight.roundToPx() }
    // 顶栏可见比例: 0=收起, 1=完全展开(enterAlways 模式下滚动后顶栏会收起)
    val currentTopBarInsetPx: () -> Int = {
        (topBarInsetPx * (1f - topAppBarScrollBehavior.state.collapsedFraction)).toInt()
    }

    val fullscreenToggle = if (LocalUISettings.current.fullscreenButtonStyle == FullscreenButtonStyle.FAB) {
        onToggleDetailPane
    } else {
        null
    }

    val scrollToTop: () -> Unit = {
        lastNavAnchorPostId = null
        navScrollActive = true
        coroutineScope.launch {
            lazyListState.scrollToItem(0)
            navScrollActive = false
            resetCommentNavDock()
        }
    }

    // 用户手动滚动时, 将导航记忆锚点对齐到当前顶部可见楼层, 使 ▲▼ 从当前位置继续,
    // 而不是回到上一次导航记忆的楼. 导航键触发的程序化滚动期间不更新, 保证连续导航.
    LaunchedEffect(lazyListState, state) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect {
            if (!navScrollActive) {
                lastNavAnchorPostId = lazyListState.navigationAnchorPost(state)?.id
            }
        }
    }

    suspend fun scrollToPost(targetIndex: Int) {
        if (targetIndex == 0) {
            // 回顶: 楼主帖本身就在顶栏下方, 不需要让位
            lazyListState.animateScrollToItem(0)
            return
        }
        if (useStickyThreadHeader) {
            // 让出顶栏 + sticky 排序栏高度
            lazyListState.scrollToItemWithHeader(
                index = targetIndex,
                scrollOffset = -currentTopBarInsetPx(),
                animate = true,
            ) { it.contentType === Type.Header }
        } else {
            lazyListState.animateScrollToItem(targetIndex, scrollOffset = -currentTopBarInsetPx())
        }
    }

    fun requestNavigateComment(direction: CommentNavDirection) {
        val anchorId = lastNavAnchorPostId?.takeIf { it in layout.orderedPostIds }
            ?: lazyListState.navigationAnchorPost(state)?.id
            ?: return
        val target = layout.targetPostId(anchorId, direction)
        if (target != null) {
            // 预加载: 目标楼接近已加载末尾且还有下一页时提前拉取, 避免到临界点再加载导致来回跳动
            if (direction == CommentNavDirection.NEXT && state.pageData.hasMore) {
                val remainingAfterTarget =
                    layout.orderedReplyPostIds.size - (layout.orderedReplyPostIds.indexOf(target) + 1)
                if (remainingAfterTarget <= NavPreloadNearEnd) {
                    viewModel.requestLoadMore()
                }
            }
            layout.itemIndexOf(target)?.let { targetIndex ->
                lastNavAnchorPostId = target
                navScrollActive = true
                coroutineScope.launch {
                    scrollToPost(targetIndex)
                    navScrollActive = false
                    resetCommentNavDock()
                }
            }
            return
        }
        // 处于边界: 决定是触发加载, 还是回跳楼主帖 / 提示已到首末楼
        when (direction) {
            CommentNavDirection.NEXT -> {
                if (state.pageData.hasMore) {
                    pendingCommentNav = PendingCommentNav(direction, anchorId)
                    viewModel.requestLoadMore()
                } else {
                    context.toastShort(R.string.tip_no_more_comment)
                }
            }
            CommentNavDirection.PREV -> {
                when {
                    anchorId == layout.firstPostId -> context.toastShort(R.string.tip_no_prev_comment)
                    state.pageData.hasPrevious -> {
                        pendingCommentNav = PendingCommentNav(direction, anchorId)
                        viewModel.requestLoadPrevious(offset = 0)
                    }
                    else -> {
                        // 无更早分页: 回跳楼主帖(此时 targetPostId 已应返回 firstPost, 兜底)
                        layout.itemIndexOf(layout.firstPostId ?: return)?.let { firstPostIndex ->
                            lastNavAnchorPostId = layout.firstPostId
                            navScrollActive = true
                            coroutineScope.launch {
                                scrollToPost(firstPostIndex)
                                navScrollActive = false
                                resetCommentNavDock()
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state) {
        val pending = pendingCommentNav ?: return@LaunchedEffect
        if (state.isLoadingMore) return@LaunchedEffect
        val newLayout = buildThreadListLayout(state)
        val target = newLayout.targetPostId(pending.anchorPostId, pending.direction)
        if (target != null) {
            lastNavAnchorPostId = target
            navScrollActive = true
            newLayout.itemIndexOf(target)?.let { scrollToPost(it) }
            navScrollActive = false
            resetCommentNavDock()
            pendingCommentNav = null
        }
    }

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openBottomSheet: () -> Unit = {
        coroutineScope.launch {
            showBottomSheet = true
            bottomSheetState.show()
        }
    }
    val closeBottomSheet: () -> Unit = {
        coroutineScope
            .launch { bottomSheetState.hide() }
            .invokeOnCompletion { showBottomSheet = false }
    }

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is CommonUiEvent.Toast -> it.message.toString()

            is CommonUiEvent.NavigateUp -> navigator.navigateUp()

            is ThreadUiEvent.DeletePostFailed -> getString(R.string.toast_delete_failure, it.message)

            is ThreadUiEvent.DeletePostSuccess -> getString(R.string.toast_delete_success)

            is ThreadUiEvent.ScrollToFirstReply -> lazyListState.scrollToItem(1)

            is ThreadUiEvent.ScrollToLatestReply -> {
                if (state.sortType != ThreadSortType.BY_DESC) {
                    lazyListState.animateScrollToItem(2 + state.data.size)
                } else {
                    lazyListState.animateScrollToItem(1)
                }
            }

            // Workaround for broken scroll position preservation
            is ThreadUiEvent.LoadPreviousSuccess -> {
                val nonDataItems = if (state.pageData.hasPrevious) 3 else 2 // FirstPost + StickyHeader + PreviousButton
                lazyListState.scrollToItem(nonDataItems + it.previousIndex, it.offset)
            }

            is ThreadUiEvent.LoadSuccess -> {
                if (it.postId != 0L || it.page > 1) {
                    lazyListState.animateScrollToItem(1)
                } else {
                    // Scroll to bottom when sorting by DESC
                    val index = if (state.sortType != ThreadSortType.BY_DESC) 1 else 2 + state.data.size
                    lazyListState.animateScrollToItem(index)
                }
            }

            is ThreadUiEvent.ToReplyDestination -> navigator.navigateDebounced(it.direction)

            is ThreadUiEvent.ToSubPostsDestination -> navigator.navigateDebounced(it.direction)

            is ThreadLikeUiEvent -> it.toMessage(context)

            is ThreadStoreUiEvent -> it.toMessage(context)

            else -> Unit
        }
        if (message is String) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(filter = { it.threadId == threadId }) { event ->
        viewModel.requestLoadMyLatestReply(event.newPostId)
    }

    if (extra != null && extra is ThreadFrom.Store && extra.maxPid != postId) {
        CollectionsUpdatedSnack(snackbarHostState, extra) {
            viewModel.requestLoad(page = 0, postId = extra.maxPid)
        }
    }

    var newMarkedCollectionPost: PostData? by remember { mutableStateOf(null) }

    newMarkedCollectionPost?.let {
        CollectionsUpdateDialog(
            markedPost = it,
            onUpdate = viewModel::updateCollections,
            onBack = navigator::navigateUp
        )
    }

    val markedDeletionPost: PostData? by viewModel.deletePost.collectAsStateWithLifecycle()
    ThreadOrPostDeleteDialog(
        deletePost = markedDeletionPost,
        firstPost = state.firstPost,
        onConfirm = viewModel::onDeleteConfirmed,
        onCancel = viewModel::onDeleteCancelled
    )

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.requestLoad(it.toInt())
        },
        dialogState = jumpToPageDialogState,
        keyboardType = KeyboardType.Number,
        isError = {
            it.isEmpty() || (it.toIntOrNull() ?: -1) !in 1..state.pageData.total
        },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            with(state.pageData) {
                Text(text = stringResource(R.string.tip_jump_page, current, total))
            }
        }
    )

    val onRefreshClicked: () -> Unit = {
        viewModel.requestLoad(0, postId)
    }

    state.thread?.let { thread ->
        LaunchedEffect(thread.like, thread.collectMarkPid, newMarkedCollectionPost?.id) {
            val markedPostId = newMarkedCollectionPost?.id ?: thread.collectMarkPid
            navigator.setResult(ThreadResultKey, createResult(threadId, thread.like, markedPostId))
        }
    }

    val onBackPressedCallback: () -> Unit = {
        if (bottomSheetState.isVisible) {
            closeBottomSheet()
        } else {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            // 更新收藏楼层
            val collectMarkPid: Long? = state.thread?.collectMarkPid
            val newCollectMarkPid: Long? = lastVisiblePost?.id
            if (collectMarkPid != null && collectMarkPid != newCollectMarkPid) {
                // Show CollectionsUpdateDialog now
                newMarkedCollectionPost = lastVisiblePost
            } else {
                if (onBack != null) onBack() else navigator.navigateUp()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            viewModel.onSaveHistory(lastVisiblePost)
        }
    }

    state.thread?.collectMarkPid?.let { collectMarkPid ->
        StrongBox {
            val interceptBack by remember {
                derivedStateOf {
                    bottomSheetState.isVisible || collectMarkPid != lazyListState.middleVisiblePost(state)?.id
                }
            }
            // 面板模式下始终接管返回键: 全屏详情先回分屏, 分屏详情先关闭详情
            BackHandler(enabled = onBack != null || interceptBack, onBack = onBackPressedCallback)
        }
    }

    StateScreen(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        isEmpty =  isEmpty,
        isLoading = state.isRefreshing,
        error = state.error,
        onReload = onRefreshClicked,
    ) {
        BlurScaffold(
            topHazeBlock = {
                blurEnabled = !topAppBarScrollBehavior.isFullyCollapsed &&
                        (lazyListState.canScrollBackward || topAppBarScrollBehavior.isOverlapping)
            },
            attachHazeContentState = false, // Attach manually since we're blurring the BottomSheet
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        state.forum?.let { forum ->
                            ForumTitleChip(forum = forum) {
                                val forumRoute = Forum(
                                    forumName = forum.second,
                                    initialThreadId = threadId,
                                    initialPostId = postId,
                                )
                                if (onOpenForum != null) onOpenForum(forumRoute)
                                else navigator.navigateDebounced(route = forumRoute)
                            }
                        }
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = onBackPressedCallback)
                    },
                    actions = {
                        if (onToggleDetailPane != null &&
                            LocalUISettings.current.fullscreenButtonStyle == FullscreenButtonStyle.TOP_BAR
                        ) {
                            ActionItem(
                                icon = if (detailPaneExpanded) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (detailPaneExpanded) {
                                    R.string.desc_collapse_detail
                                } else {
                                    R.string.desc_expand_detail
                                }
                            ) {
                                onToggleDetailPane()
                            }
                        }
                        val scrollToTopVisible by remember { // Not on top or Toolbar is collapsed
                            derivedStateOf { lazyListState.canScrollBackward || toolbarScrollBehavior.state.collapsedFraction >= 0.9f }
                        }
                        FadedVisibility(visible = scrollToTopVisible) {
                            ActionItem(
                                icon = Icons.Rounded.VerticalAlignTop,
                                contentDescription = R.string.btn_back_to_top
                            ) {
                                if (scrollToTopVisible) {
                                    coroutineScope.launch { lazyListState.scrollToItem(0) }
                                    topAppBarScrollBehavior.state.contentOffset = 0f
                                    toolbarScrollBehavior.state.contentOffset = 0f
                                    toolbarScrollBehavior.state.offset = 0f
                                }
                            }
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                ) {
                    if (useStickyHeaderWorkaround && state.thread?.replyNum != null) {
                        Container {
                            StickyHeaderOverlay(state = lazyListState) {
                                ThreadHeader(uiState = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (viewModel.hideReply) {
                    val isLeft =
                        LocalUISettings.current.compactReplyBarPosition == CompactReplyBarPosition.LEFT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .offset(y = -ThreadToolbarScreenOffset),
                    ) {
                        ThreadFloatingToolbar(
                            compact = true,
                            modifier = Modifier
                                .align(if (isLeft) Alignment.BottomStart else Alignment.BottomEnd)
                                .padding(horizontal = CardHorizontalSpacing)
                                .animateEnterExit(
                                    animatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                                    sharedTransitionScope = LocalSharedTransitionScope.current,
                                    enter = defaultVerticalEnterTransition(topToBottom = false),
                                    exit = defaultVerticalExitTransition(topToBottom = false),
                                ),
                            onClickMore = openBottomSheet,
                            like = state.thread?.like ?: LikeZero,
                            onLiked = viewModel::onThreadLikeClicked,
                            scrollBehavior = toolbarScrollBehavior,
                        )
                    }
                } else {
                    Container {
                        ThreadFloatingToolbar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .offset(y = -ThreadToolbarScreenOffset)
                                .padding(horizontal = CardHorizontalSpacing)
                                .animateEnterExit(
                                    animatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                                    sharedTransitionScope = LocalSharedTransitionScope.current,
                                    enter = defaultVerticalEnterTransition(topToBottom = false),
                                    exit = defaultVerticalExitTransition(topToBottom = false),
                                ),
                            user = state.user,
                            onClickReply = viewModel::onReplyThread.takeUnless { viewModel.hideReply },
                            onClickMore =  openBottomSheet,
                            onJumpPage = jumpToPageDialogState::show,
                            like = state.thread?.like ?: LikeZero,
                            onLiked = viewModel::onThreadLikeClicked,
                            scrollBehavior = toolbarScrollBehavior
                        )
                    }
                }
            },
            bottomHazeBlock = { blurEnabled = false },
            snackbarHostState = snackbarHostState,
            snackbarHost = { SwipeToDismissSnackbarHost(hostState = snackbarHostState) },
            backgroundColor = Color.Transparent,
        ) { padding ->
            val hazeState = LocalHazeState.current
            // Ignore Scaffold padding top changes if workaround enabled
            val contentPadding = padding.fixedTopBarPadding()

            Box(modifier = Modifier.fillMaxSize()) {
                Container(modifier = Modifier.clipToBounds()) {
                    ProvideNavigator(navigator = navigator) {
                        ThreadContent(
                            modifier = Modifier
                                .hazeSource(hazeState?.state)
                                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                .nestedScroll(toolbarScrollBehavior),
                            viewModel = viewModel,
                            lazyListState = lazyListState,
                            contentPadding = contentPadding,
                            topAppBarScrollBehavior = topAppBarScrollBehavior,
                            layout = layout,
                            useStickyHeader = useStickyHeader && !useStickyHeaderWorkaround
                        )
                    }
                }

                val commentNavEnabled = LocalUISettings.current.commentNavEnabled
                if (commentNavEnabled || fullscreenToggle != null) {
                    ThreadNavigationDock(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = CardHorizontalSpacing,
                                bottom = padding.calculateBottomPadding() +
                                        ThreadToolbarScreenOffset +
                                        CardHorizontalSpacing,
                            ),
                        onPrev = { requestNavigateComment(CommentNavDirection.PREV) },
                        onNext = { requestNavigateComment(CommentNavDirection.NEXT) },
                        showCommentNav = commentNavEnabled,
                        hideCommentNav = hideCommentNav,
                        onPrevLongPress = scrollToTop,
                        onToggleDetailPane = fullscreenToggle,
                        detailPaneExpanded = detailPaneExpanded,
                    )
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = bottomSheetState,
                    containerColor = Color.Transparent, // Set background for blurring
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = Color.Transparent,
                    dragHandle = null,
                    contentWindowInsets = { NoWindowInsets } // Handle it inside the content for blurring
                ) {
                    val isMyThread by remember(state.lz) {
                        derivedStateOf { state.user != null && state.lz?.id == state.user?.id }
                    }
                    val isDesc by remember { derivedStateOf { state.sortType == ThreadSortType.BY_DESC } }

                    ThreadMenu(
                        isSeeLz = state.seeLz,
                        isCollected = state.thread?.collected == true,
                        isImmersiveMode = viewModel.isImmersiveMode,
                        isDesc = isDesc,
                        onSeeLzClick = viewModel::onSeeLzChanged,
                        onCollectClick = {
                            if (state.user == null) {
                                context.toastShort(R.string.title_not_logged_in)
                            } else if (state.thread!!.collected) {
                                viewModel.removeFromCollections()
                            } else {
                                lazyListState.middleVisiblePost(state)?.let { post ->
                                    viewModel.updateCollections(markedPost = post)
                                }
                            }
                        },
                        onImmersiveModeClick = {
                            if (!viewModel.isImmersiveMode && !state.seeLz) {
                                viewModel.onSeeLzChanged()
                            }
                            viewModel.onImmersiveModeChanged()
                        },
                        onDescClick = {
                            val notDesc = state.sortType != ThreadSortType.BY_DESC
                            val sortType = if (notDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT
                            viewModel.onSortChanged(sortType)
                        },
                        onShareClick = viewModel::onShareThread,
                        onCopyLinkClick = viewModel::onCopyThreadLink,
                        onReportClick = {
                            coroutineScope.launch {
                                TiebaUtil.reportPost(context, navigator, state.firstPost!!.id.toString())
                            }
                        },
                        onDeleteClick = viewModel::onDeleteThread.takeIf { isMyThread },
                        requestCloseMenu = closeBottomSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                            )
                            .withNonNull(hazeState) { Modifier.defaultHazeEffect() }
                            .background(TiebaLiteTheme.extendedColorScheme.sheetContainerColor)
                            .padding(top = 16.dp)
                            .windowInsetsPadding(BottomSheetDefaults.windowInsets)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumTitleChip(forum: SimpleForum, onForumClick: () -> Unit) {
    Surface(
        onClick = onForumClick,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = forum.second
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.third,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )

            val forumStyle = MaterialTheme.typography.titleMedium
            Text(
                text = stringResource(id = R.string.title_forum, forum.second),
                modifier = Modifier.padding(horizontal = 8.dp),
                autoSize = TextAutoSize.StepBased(8.sp, forumStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = forumStyle
            )
        }
    }
}

@Composable
private fun ThreadMenu(
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    requestCloseMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(0.2f)
                .background(color = MaterialTheme.colorScheme.onSurfaceVariant, shape = CircleShape)
        )
        VerticalGrid(
            column = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            rowModifier = Modifier.height(IntrinsicSize.Min),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_see_lz),
                    checked = isSeeLz,
                    onClick = {
                        requestCloseMenu()
                        onSeeLzClick()
                    },
                    icon = if (isSeeLz) Icons.Rounded.Face6 else Icons.Rounded.FaceRetouchingOff,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = if (isCollected) R.string.title_collected else R.string.title_uncollected),
                    checked = isCollected,
                    onClick = {
                        requestCloseMenu()
                        onCollectClick()
                    },
                    icon = if (isCollected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_pure_read),
                    checked = isImmersiveMode,
                    onClick = {
                        requestCloseMenu()
                        onImmersiveModeClick()
                    },
                    icon = if (isImmersiveMode) Icons.AutoMirrored.Rounded.ChromeReaderMode else Icons.AutoMirrored.Outlined.ChromeReaderMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_sort),
                    checked = isDesc,
                    onClick = {
                        requestCloseMenu()
                        onDescClick()
                    },
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column {
            ListMenuItem(
                icon = Icons.Rounded.Share,
                text = stringResource(id = R.string.title_share),
                onClick = {
                    requestCloseMenu()
                    onShareClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.ContentCopy,
                text = stringResource(id = R.string.title_copy_link),
                onClick = {
                    requestCloseMenu()
                    onCopyLinkClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Report,
                text = stringResource(id = R.string.title_report),
                onClick = {
                    requestCloseMenu()
                    onReportClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDeleteClick != null) {
                ListMenuItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(id = R.string.title_delete),
                    onClick = {
                        requestCloseMenu()
                        onDeleteClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CollectionsUpdatedSnack(
    snackbarHostState: SnackbarHostState,
    extra: ThreadFrom.Store,
    onLoadLatest: () -> Unit
) {
    var showed by rememberSaveable { mutableStateOf(false) }
    if (showed) return // Display only once

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.message_store_thread_update, extra.maxFloor),
            context.getString(R.string.button_load_new),
            true,
            SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            onLoadLatest()
        }
        showed = true
    }
}

@Composable
private fun CollectionsUpdateDialog(markedPost: PostData, onUpdate: (PostData) -> Unit, onBack: () -> Unit) {
    val updateCollectMarkDialogState = rememberDialogState()
    LaunchedEffect(markedPost) {
        updateCollectMarkDialogState.show()
    }

    if (!updateCollectMarkDialogState.show) return
    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            onUpdate(markedPost)
        },
        onDismiss = onBack,
    ) {
        Text(stringResource(R.string.message_update_collect_mark, markedPost.floor))
    }
}

@Composable
private fun ThreadOrPostDeleteDialog(
    deletePost: PostData?,
    firstPost: PostData?,
    onConfirm: () -> Job,
    onCancel: () -> Unit
) {
    val dialogState = rememberDialogState()
    LaunchedEffect(deletePost) {
        if (deletePost != null) dialogState.show()
    }

    if (!dialogState.show || firstPost == null) return

    var deleting by remember { mutableStateOf(false) }

    Dialog(
        dialogState = dialogState,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(text = stringResource(R.string.title_delete)) },
        buttons = {
            AnimatedVisibility(visible = !deleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    DialogNegativeButton(text = stringResource(R.string.button_cancel), onClick = onCancel)

                    Button(
                        onClick = {
                            deleting = true
                            onConfirm().invokeOnCompletion {
                                dismiss()
                                deleting = false
                            }
                        },
                        content = { Text(text = stringResource(R.string.button_sure)) }
                    )
                }
            }
        }
    ) {
        if (deletePost == null || deleting) {
            Text(text = stringResource(id = R.string.dialog_content_wait))
        } else {
            val postType = if (deletePost.id == firstPost.id) {
                stringResource(id = R.string.this_thread)
            } else {
                stringResource(R.string.tip_post_floor, deletePost.floor)
            }
            Text(text = stringResource(id = R.string.message_confirm_delete, postType))
        }
    }
}

@Composable
private fun ThreadFloatingToolbar(
    modifier: Modifier = Modifier,
    user: UserData? = null,
    onClickReply: (() -> Unit)? = null,
    onClickMore: () -> Unit = {},
    onJumpPage: () -> Unit = {},
    like: Like = LikeZero,
    onLiked: () -> Unit = {},
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
    compact: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    // Default: FloatingToolbarTokens.VibrantContainerColor
    val toolbarContainerColor = colorScheme.primaryContainer.let {
        if (!colorScheme.isTranslucent && !LocalUISettings.current.reduceEffect) it.copy(alpha = 0.7f) else it
    }

    ProvideContentColor(colorScheme.onPrimaryContainer) {
        Row(
            modifier = modifier
                .onNotNull(scrollBehavior) {
                    with(it) { floatingScrollBehavior() }
                }
                .height(ThreadToolbarContainerHeight)
                .graphicsLayer {
                    this.shadowElevation = shadowElevation.toPx()
                    this.shape = CircleShape
                    this.clip = true
                }
                .withNonNull(LocalHazeState.current) { Modifier.defaultHazeEffect() }
                .background(color = toolbarContainerColor, shape = CircleShape)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) {
                val avatarContentDescription = user?.name ?: stringResource(R.string.title_not_logged_in)
                PlainTooltipBox(
                    positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    contentDescription = avatarContentDescription
                ) {
                    Avatar(
                        modifier = Modifier.size(40.dp),
                        data = user?.avatarUrl ?: R.drawable.ic_launcher_new_round,
                        contentDescription = avatarContentDescription
                    )
                }

                if (onClickReply != null) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1.0f)
                            .clickableNoIndication(onClick = onClickReply),
                        color = LocalContentColor.current.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                ActionItem(
                    icon = Icons.Rounded.RocketLaunch,
                    contentDescription = stringResource(R.string.title_jump_page),
                    positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    onClick = onJumpPage,
                )
            }

            LikeAction(like = like, onClick = onLiked)

            ActionItem(
                icon = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.btn_more),
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                onClick = onClickMore
            )
        }
    }
}

@Composable
private fun LikeAction(modifier: Modifier = Modifier, like: Like, onClick: () -> Unit) {
    val contentDescription = stringResource(R.string.button_like)
    PlainTooltipBox(
        modifier = modifier,
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        BadgedBox(
            badge = {
                if (like.count > 0) {
                    Surface(
                        modifier = Modifier.graphicsLayer {
                            translationX = -size.width * if (like.count > 999) 0.45f else 0.3f
                            translationY = size.height * 0.25f
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Text(
                            text = remember(like.count) { like.count.getShortNumString() },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            autoSize = TextAutoSize.StepBased(4.sp, 9.sp),
                            lineHeight = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            },
        ) {
            val animatedColor by animateColorAsState(
                targetValue = if (like.liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (like.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                    tint = animatedColor
                )
            }
        }
    }
}

@Preview("LikeAction")
@Composable
private fun LikeActionPreview() = TiebaLiteTheme {
    LikeAction(like = Like(liked = true, count = 999)) { }
}
