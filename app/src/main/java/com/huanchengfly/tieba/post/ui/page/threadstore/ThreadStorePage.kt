package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SharedTransitionUserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ThreadStorePage(
    navigator: NavController,
    viewModel: ThreadStoreViewModel = hiltViewModel()
) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_my_collect),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
            )
        },
    ) { contentPadding ->
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current

        val isRefreshing by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isRefreshing,
            initial = false
        )
        val isEmpty by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isEmpty,
            initial = true
        )

        val error by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::error,
            initial = null
        )

        viewModel.uiEvent.collectUiEventWithLifecycle { event ->
            val message = when(event) {
                is ThreadStoreUiEvent -> event.toMessage(context)

                is CommonUiEvent.Toast -> event.message.toString()

                else -> Unit
            }
            if (message is String) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }

        val data by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::data,
            initial = emptyList()
        )

        // 列表状态不进 saveable: 进程恢复/刷新重置后一律从顶部开始,
        // 从帖子返回时按 VM 记录的滚动位置手动恢复
        // (LazyListState 的 saveable 恢复会把旧位置落进重置后的短列表, 且不受 key() 控制)
        val listState = remember { LazyListState() }
        var canSaveListPosition by remember { mutableStateOf(false) }
        LaunchedEffect(listState) {
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (canSaveListPosition) {
                    viewModel.saveListPosition(index, offset)
                }
            }
        }
        LaunchedEffect(data.isNotEmpty()) {
            if (data.isNotEmpty() && viewModel.listRestoreIndex > 0) {
                listState.scrollToItem(viewModel.listRestoreIndex, viewModel.listRestoreOffset)
            }
            canSaveListPosition = true
        }

        StateScreen(
            isEmpty = isEmpty,
            isLoading = isRefreshing,
            error = error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPadding,
        ) {
            val isLoadingMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::isLoadingMore,
                initial = false
            )
            val hasMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::hasMore,
                initial = true
            )

            val habit = LocalHabitSettings.current

            // Initialize click listeners now
            val onUserClicked: (Author, String) -> Unit = { author, extraKey ->
                val route = author.run { UserProfile(id, avatarUrl, name, transitionKey = extraKey) }
                navigator.navigateDebounced(route)
            }

            val onThreadClicked: (ThreadStore) -> Unit = { thread ->
                navigator.navigateDebounced(
                    route = Thread(
                        threadId = thread.id,
                        postId = thread.markPid,
                        seeLz = habit.favoriteSeeLz,
                        sortType = if (habit.favoriteDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT,
                        from = ThreadFrom.Store(maxPid = thread.maxPid, maxFloor = thread.postNo)
                    )
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                // onLoad 置空: 下拉手势让给外层 PullToRefreshBox 触发刷新,
                // 否则 SwipeUp 连接会抢走手势触发加载更多
                SwipeUpLazyLoadColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                    isLoading = isLoadingMore,
                    onLazyLoad = viewModel::onLoadMore.takeIf { hasMore },
                    bottomIndicator = {
                        LoadMoreIndicator(noMore = !hasMore, onThreshold = it)
                    }
                ) {
                    items(items = data, key = { it.id }) { info ->
                        StoreItem(
                            info = info,
                            onUserClick = onUserClicked,
                            onClick = onThreadClicked,
                            onDelete = viewModel::onDelete
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            navigator.consumeResult<Destination.ThreadStore, ThreadResult>(ThreadResultKey)?.run {
                viewModel.onThreadResult(threadId, markedPostId)
            }
        }
    }
}

@Composable
private fun StoreItem(
    info: ThreadStore,
    onUserClick: (Author, transitionKey: String) -> Unit,
    onDelete: (ThreadStore) -> Unit,
    onClick: (ThreadStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = info.count != 0 && info.postNo != 0

    LongClickMenu(
        menuContent = {
            TextMenuItem(text = R.string.title_collect_on, onClick = { onDelete(info) })
        },
        onClick = { onClick(info) }
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SharedTransitionUserHeader(
                user = info.author,
                extraKey = info.id,
                desc = if (hasUpdate) {
                    stringResource(id = R.string.tip_thread_store_update, info.postNo)
                } else {
                    null
                },
                onClick = { onUserClick(info.author, info.id.toString()) },
            ) {
                Spacer(Modifier.weight(1.0f))

                Surface (
                    shape = MaterialTheme.shapes.extraSmall,
                    color = colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum_name, info.forumName),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text(
                text = info.title,
                color = if (info.isDeleted) colorScheme.outlineVariant else colorScheme.onSurface,
                fontSize = 15.sp,
                textDecoration = if (info.isDeleted) TextDecoration.LineThrough else null
            )

            if (info.isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    fontSize = 12.sp,
                    color = colorScheme.outlineVariant
                )
            }
        }
    }
}