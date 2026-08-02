package com.huanchengfly.tieba.post.ui.page.user.followlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.FollowListBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.persistentListOf

private enum class FollowListFilter { All, Mutual }

@Composable
fun FollowListPage(
    uid: Long = 0,
    navigator: NavController,
    viewModel: FollowListViewModel = pageViewModel(),
) {
    val showActions = (uid == 0L || uid == LocalAccount.current?.uid)
    val lazyListState = rememberLazyListState()

    fun refresh() {
        if (showActions) {
            viewModel.send(FollowListUiIntent.Refresh())
        } else {
            viewModel.send(FollowListUiIntent.Refresh(uid))
        }
    }

    LazyLoad(loaded = viewModel.initialized) {
        refresh()
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::hasMore,
        initial = false
    )
    val totalFollowNum by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::totalFollowNum,
        initial = 0
    )
    val tipsText by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::tipsText,
        initial = null
    )
    val users by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::users,
        initial = persistentListOf()
    )
    val unfollowedIds by viewModel.uiState.collectPartialAsState(
        prop1 = FollowListUiState::unfollowedIds,
        initial = emptySet()
    )

    var filter by rememberSaveable { mutableStateOf(FollowListFilter.All) }

    val isEmpty by remember {
        derivedStateOf { users.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    val displayUsers = remember(users, filter) {
        when (filter) {
            FollowListFilter.All -> users
            FollowListFilter.Mutual -> users.filter { it.hasConcerned == 2 }
        }
    }

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_follow_list),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        }
    ) { contentPaddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
        ) {
            FollowListHeader(
                filter = filter,
                onFilterChange = { filter = it },
                totalFollowNum = totalFollowNum,
                tipsText = tipsText,
                showFilter = showActions,
            )

            StateScreen(
                modifier = Modifier.fillMaxWidth().weight(1f),
                isEmpty = isEmpty,
                isError = isError,
                isLoading = isRefreshing,
                onReload = ::refresh,
                errorScreen = { ErrorScreen(error = error.getOrNull()) },
            ) {
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isRefreshing,
                    onRefresh = ::refresh
                ) {
                    SwipeUpLazyLoadColumn(
                        state = lazyListState,
                        isLoading = isLoadingMore,
                        onLazyLoad = {
                            if (hasMore) {
                                if (showActions) {
                                    viewModel.send(FollowListUiIntent.LoadMore(currentPage))
                                } else {
                                    viewModel.send(FollowListUiIntent.LoadMore(currentPage, uid))
                                }
                            }
                        },
                        bottomIndicator = { defaultBottomIndicator(it) }
                    ) {
                        followList(
                            data = displayUsers,
                            unfollowedIds = unfollowedIds,
                            showButton = showActions,
                            onUnfollow = { item ->
                                val portrait = item.portrait
                                val tbs = AccountUtil.getAccountInfo { this.tbs }
                                if (portrait != null && tbs != null) {
                                    viewModel.send(
                                        FollowListUiIntent.Unfollow(
                                            item.id,
                                            portrait,
                                            tbs
                                        )
                                    )
                                }
                            },
                            onFollow = { item ->
                                val portrait = item.portrait
                                val tbs = AccountUtil.getAccountInfo { this.tbs }
                                if (portrait != null && tbs != null) {
                                    viewModel.send(
                                        FollowListUiIntent.Follow(
                                            item.id,
                                            portrait,
                                            tbs
                                        )
                                    )
                                }
                            },
                            onClick = { item ->
                                navigator.navigateDebounced(Destination.UserProfile(uid = item.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowListHeader(
    filter: FollowListFilter,
    onFilterChange: (FollowListFilter) -> Unit,
    totalFollowNum: Int,
    tipsText: String?,
    showFilter: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.text_follow_list_count, totalFollowNum),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showFilter) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = MaterialTheme.colorScheme.primary)
                            .clickable { expanded = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(
                                id = if (filter == FollowListFilter.All) {
                                    R.string.filter_follow_all
                                } else {
                                    R.string.filter_follow_mutual
                                }
                            ),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                onFilterChange(FollowListFilter.All)
                                expanded = false
                            },
                            text = {
                                Text(text = stringResource(id = R.string.filter_follow_all))
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                onFilterChange(FollowListFilter.Mutual)
                                expanded = false
                            },
                            text = {
                                Text(text = stringResource(id = R.string.filter_follow_mutual))
                            }
                        )
                    }
                }
            }
        }
        tipsText?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

private fun LazyListScope.followList(
    data: List<FollowListBean.FollowUserBean>,
    unfollowedIds: Set<Long>,
    showButton: Boolean = true,
    onUnfollow: (FollowListBean.FollowUserBean) -> Unit,
    onFollow: (FollowListBean.FollowUserBean) -> Unit,
    onClick: (FollowListBean.FollowUserBean) -> Unit,
) {
    items(
        items = data,
        key = { it.id }
    ) {
        val unfollowed = it.id in unfollowedIds
        FollowListItem(
            item = it,
            unfollowed = unfollowed,
            showButton = showButton,
            onButtonClick = {
                if (unfollowed) {
                    onFollow(it)
                } else {
                    onUnfollow(it)
                }
            },
            onClick = { onClick(it) }
        )
    }
}

@Composable
private fun FollowListItem(
    item: FollowListBean.FollowUserBean,
    unfollowed: Boolean,
    showButton: Boolean = true,
    onButtonClick: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            data = StringUtil.getAvatarUrl(item.portrait),
            size = Sizes.Small,
            contentDescription = item.name
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = StringUtil.getUserNameString(
                    showBoth = true,
                    username = item.name.orEmpty(),
                    nickname = item.nameShow
                ),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subText = item.intro
            if (!subText.isNullOrBlank()) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (showButton) {
            FollowListFollowButton(
                text = when {
                    unfollowed -> stringResource(id = R.string.button_follow)
                    item.hasConcerned == 2 -> stringResource(id = R.string.filter_follow_mutual)
                    else -> stringResource(id = R.string.text_followed)
                },
                followed = !unfollowed,
                onClick = onButtonClick,
            )
        }
    }
}

@Composable
private fun FollowListFollowButton(
    text: String,
    followed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (followed) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary
    val contentColor =
        if (followed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color = background)
            .clickable(onClick = onClick)
            .widthIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
