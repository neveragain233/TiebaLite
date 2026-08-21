package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedThreadItem
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = hiltViewModel(),
    onOpenThread: ((Destination.Thread) -> Unit)? = null,
    pageIndex: Int,
) {
    val coroutineScope = rememberCoroutineScope()

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    // 长按回顶键触发的刷新请求, 只处理属于本页的索引
    onGlobalEvent<GlobalEvent.RefreshExplore>(coroutineScope, filter = { it.pageIndex == pageIndex }) {
        viewModel.onRefresh()
    }

    LaunchedFabStateEffect(listState, onHideFab, isRefreshing, isError = error != null)

    val threadClickListeners = remember(navigator, onOpenThread) {
        createThreadClickListeners(
            onNavigate = navigator::navigateDebounced,
            onOpenThread = onOpenThread,
        )
    }

    ConsumeThreadPageResult<Destination.Main>(navigator, viewModel::onThreadResult)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val data = uiState.data
            val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    FeedThreadItem(
                        thread = thread,
                        hideBlockedContent = hideBlockedContent,
                        onUndoHidden = viewModel::unhideThread,
                    ) {
                        FeedCard(
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onLike = viewModel::onThreadLikeClicked,
                            onClickReply = threadClickListeners.onReplyClicked,
                            onClickUser = threadClickListeners.onAuthorClicked,
                            onClickForum = threadClickListeners.onForumClicked,
                            onHide = viewModel::hideThread,
                            cardDivider = i < data.lastIndex
                        )
                    }
                }
            }
        }
    }
}
