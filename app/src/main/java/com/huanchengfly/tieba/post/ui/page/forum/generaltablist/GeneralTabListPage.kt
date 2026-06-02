package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.TopThreadItem
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun GeneralTabListPage(
    forumId: Long,
    forumName: String,
    navTabInfo: FrsTabInfo,
    viewModel: GeneralTabListViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigateDebounced)
    }

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            GeneralTabListUiIntent.FirstLoad(
                forumId = forumId,
                forumName = forumName,
                navTabInfo = navTabInfo,
                sortType = navTabInfo.sort_menu.firstOrNull()?.source_id ?: -1,
            )
        )
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::currentPage,
        initial = 1
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::threadList,
        initial = persistentListOf()
    )
    val sortType by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::sortType,
        initial = -1
    )

    val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()

    onGlobalEvent<GeneralTabListUiEvent.BackToTop> {
        lazyListState.animateScrollToItem(0)
    }
    onGlobalEvent<GeneralTabListUiEvent.Refresh> { event ->
        viewModel.send(
            GeneralTabListUiIntent.Refresh(
                forumId = forumId,
                forumName = forumName,
                navTabInfo = navTabInfo,
                sortType = event.sortType.takeIf { it >= 0 } ?: sortType,
            )
        )
    }
    viewModel.onEvent<GeneralTabListUiEvent.AgreeFail> {
        coroutineScope.launch {
            val snackbarResult = snackbarHostState.showSnackbar(
                message = context.getString(
                    R.string.snackbar_agree_fail,
                    it.errorCode,
                    it.errorMsg
                ),
                actionLabel = context.getString(R.string.button_retry)
            )
            if (snackbarResult == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    GeneralTabListUiIntent.Agree(
                        threadId = it.threadId,
                        postId = it.postId,
                        hasAgree = it.hasAgree,
                    )
                )
            }
        }
    }

    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.send(
                GeneralTabListUiIntent.Refresh(
                    forumId = forumId,
                    forumName = forumName,
                    navTabInfo = navTabInfo,
                    sortType = sortType,
                )
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (navTabInfo.sub_tab_list.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = navTabInfo.sub_tab_list,
                        key = { it.class_id }
                    ) { menu ->
                        Chip(
                            text = menu.class_name,
                            invertColor = false,
                            onClick = { }
                        )
                    }
                }
            }

            SwipeUpLazyLoadColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                isLoading = isLoadingMore,
                onLazyLoad = {
                    viewModel.send(
                        GeneralTabListUiIntent.LoadMore(
                            forumId = forumId,
                            forumName = forumName,
                            navTabInfo = navTabInfo,
                            currentPage = currentPage,
                            lastThreadId = threadList.lastOrNull()?.id ?: 0,
                            sortType = sortType,
                        )
                    )
                }.takeIf { hasMore },
                bottomIndicator = {
                    LoadMoreIndicator(noMore = !hasMore, onThreshold = it)
                }
            ) {
                ThreadList(
                    items = threadList,
                    hideBlocked = hideBlocked,
                    onItemClicked = threadClickListeners.onClicked,
                    onItemReplyClicked = threadClickListeners.onReplyClicked,
                    onAgree = { threadInfo ->
                        viewModel.send(
                            GeneralTabListUiIntent.Agree(
                                threadId = threadInfo.id,
                                postId = threadInfo.firstPostId,
                                hasAgree = if (threadInfo.liked) 1 else 0,
                            )
                        )
                    },
                    onUserClicked = threadClickListeners.onAuthorClicked,
                    onClickOriginThread = {
                        navigator.navigateDebounced(
                            Destination.Thread(threadId = it.tid.toLong(), forumId = it.fid)
                        )
                    }
                )
            }
        }
    }
}

private fun LazyListScope.ThreadList(
    items: List<ThreadItem>,
    hideBlocked: Boolean,
    onItemClicked: (ThreadItem) -> Unit,
    onItemReplyClicked: (ThreadItem) -> Unit,
    onAgree: (ThreadItem) -> Unit,
    onUserClicked: (ThreadItem) -> Unit,
    onClickOriginThread: (OriginThreadInfo) -> Unit = {},
) {
    // MyLazyColumn() {
        itemsIndexed(items = items, key = { _, it -> it.id }, ThreadContentType) { index, item ->
            BlockableContent(
                blocked = item.blocked,
                blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                hideBlockedContent = hideBlocked,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (item.isTop) {
                        TopThreadItem(
                            title = item.title,
                            onClick = { onItemClicked(item) }
                        )
                    } else {
                        if (index > 0) {
                            if (items[index - 1].isTop) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        FeedCard(
                            thread = item,
                            onClick = onItemClicked,
                            onLike = onAgree,
                            onClickReply = onItemReplyClicked,
                            onClickUser = onUserClicked,
                            onClickOriginThread = onClickOriginThread,
                        )
                    }
                }
            }
        // }
    }
}
