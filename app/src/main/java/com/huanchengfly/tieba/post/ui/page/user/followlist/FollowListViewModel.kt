package com.huanchengfly.tieba.post.ui.page.user.followlist

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.FollowBean
import com.huanchengfly.tieba.post.api.models.FollowListBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class FollowListViewModel @Inject constructor() :
    BaseViewModel<FollowListUiIntent, FollowListPartialChange, FollowListUiState, UiEvent>() {
    override fun createInitialState(): FollowListUiState = FollowListUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<FollowListUiIntent, FollowListPartialChange, FollowListUiState> =
        FollowListPartialChangeProducer

    override fun dispatchEvent(partialChange: FollowListPartialChange): UiEvent? =
        when (partialChange) {
            is FollowListPartialChange.Unfollow.Success -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_follow_list_unfollow_success)
            )

            is FollowListPartialChange.Follow.Success -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_follow_list_follow_success)
            )

            is FollowListPartialChange.Unfollow.Failure -> CommonUiEvent.Toast(
                App.INSTANCE.getString(
                    R.string.toast_unlike_failed,
                    partialChange.error.getErrorMessage()
                )
            )

            else -> null
        }

    private object FollowListPartialChangeProducer :
        PartialChangeProducer<FollowListUiIntent, FollowListPartialChange, FollowListUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<FollowListUiIntent>): Flow<FollowListPartialChange> =
            merge(
                intentFlow.filterIsInstance<FollowListUiIntent.Refresh>()
                    .flatMapConcat { it.toRefreshPartialChangeFlow() },
                intentFlow.filterIsInstance<FollowListUiIntent.LoadMore>()
                    .flatMapConcat { it.toLoadMorePartialChangeFlow() },
                intentFlow.filterIsInstance<FollowListUiIntent.Unfollow>()
                    .flatMapConcat { it.toUnfollowPartialChangeFlow() },
                intentFlow.filterIsInstance<FollowListUiIntent.Follow>()
                    .flatMapConcat { it.toFollowPartialChangeFlow() },
            )

        private fun FollowListUiIntent.Refresh.toRefreshPartialChangeFlow(): Flow<FollowListPartialChange.Refresh> =
            TiebaApi.getInstance().followListFlow(uid = uid)
                .map<FollowListBean, FollowListPartialChange.Refresh> {
                    FollowListPartialChange.Refresh.Success(
                        page = 1,
                        hasMore = it.hasMore == 1,
                        totalFollowNum = it.totalFollowNum,
                        tipsText = it.tipsText,
                        users = it.followList,
                    )
                }
                .onStart { emit(FollowListPartialChange.Refresh.Start) }
                .catch { emit(FollowListPartialChange.Refresh.Failure(it)) }

        private fun FollowListUiIntent.LoadMore.toLoadMorePartialChangeFlow(): Flow<FollowListPartialChange.LoadMore> =
            TiebaApi.getInstance().followListFlow(page + 1, uid)
                .map<FollowListBean, FollowListPartialChange.LoadMore> {
                    FollowListPartialChange.LoadMore.Success(
                        page = it.pageNum,
                        hasMore = it.hasMore == 1,
                        users = it.followList,
                    )
                }
                .onStart { emit(FollowListPartialChange.LoadMore.Start) }
                .catch { emit(FollowListPartialChange.LoadMore.Failure(it)) }

        private fun FollowListUiIntent.Unfollow.toUnfollowPartialChangeFlow(): Flow<FollowListPartialChange.Unfollow> =
            TiebaApi.getInstance().unfollowFlow(portrait, tbs)
                .map<CommonResponse, FollowListPartialChange.Unfollow> {
                    FollowListPartialChange.Unfollow.Success(userId)
                }
                .onStart { emit(FollowListPartialChange.Unfollow.Start) }
                .catch { emit(FollowListPartialChange.Unfollow.Failure(it)) }

        private fun FollowListUiIntent.Follow.toFollowPartialChangeFlow(): Flow<FollowListPartialChange.Follow> =
            TiebaApi.getInstance().followFlow(portrait, tbs)
                .map<FollowBean, FollowListPartialChange.Follow> {
                    FollowListPartialChange.Follow.Success(userId)
                }
                .onStart { emit(FollowListPartialChange.Follow.Start) }
                .catch { emit(FollowListPartialChange.Follow.Failure(it)) }
    }
}

sealed interface FollowListUiIntent : UiIntent {
    data class Refresh(val uid: Long? = null) : FollowListUiIntent
    data class LoadMore(val page: Int, val uid: Long? = null) : FollowListUiIntent
    data class Unfollow(
        val userId: Long,
        val portrait: String,
        val tbs: String,
    ) : FollowListUiIntent
    data class Follow(
        val userId: Long,
        val portrait: String,
        val tbs: String,
    ) : FollowListUiIntent
}

sealed interface FollowListPartialChange : PartialChange<FollowListUiState> {
    sealed class Refresh : FollowListPartialChange {
        override fun reduce(oldState: FollowListUiState): FollowListUiState = when (this) {
            is Start -> oldState.copy(isRefreshing = true)

            is Success -> oldState.copy(
                isRefreshing = false,
                error = null,
                currentPage = page,
                hasMore = hasMore,
                totalFollowNum = totalFollowNum,
                tipsText = tipsText,
                users = users.toImmutableList(),
                unfollowedIds = emptySet(),
            )

            is Failure -> oldState.copy(
                isRefreshing = false,
                error = error.wrapImmutable(),
            )
        }

        data object Start : Refresh()

        data class Success(
            val page: Int,
            val hasMore: Boolean,
            val totalFollowNum: Int,
            val tipsText: String?,
            val users: List<FollowListBean.FollowUserBean>,
        ) : Refresh()

        data class Failure(val error: Throwable) : Refresh()
    }

    sealed class LoadMore : FollowListPartialChange {
        override fun reduce(oldState: FollowListUiState): FollowListUiState = when (this) {
            is Start -> oldState.copy(isLoadingMore = true)

            is Success -> oldState.copy(
                isLoadingMore = false,
                error = null,
                currentPage = page,
                hasMore = hasMore,
                users = (oldState.users + users).distinctBy { it.id }.toImmutableList(),
            )

            is Failure -> oldState.copy(
                isLoadingMore = false,
                error = error.wrapImmutable(),
            )
        }

        data object Start : LoadMore()

        data class Success(
            val page: Int,
            val hasMore: Boolean,
            val users: List<FollowListBean.FollowUserBean>,
        ) : LoadMore()

        data class Failure(val error: Throwable) : LoadMore()
    }

    sealed class Unfollow : FollowListPartialChange {
        override fun reduce(oldState: FollowListUiState): FollowListUiState = when (this) {
            is Start -> oldState

            is Success -> oldState.copy(
                unfollowedIds = oldState.unfollowedIds + userId,
            )

            is Failure -> oldState
        }

        data object Start : Unfollow()

        data class Success(val userId: Long) : Unfollow()

        data class Failure(val error: Throwable) : Unfollow()
    }

    sealed class Follow : FollowListPartialChange {
        override fun reduce(oldState: FollowListUiState): FollowListUiState = when (this) {
            is Start -> oldState

            is Success -> oldState.copy(
                unfollowedIds = oldState.unfollowedIds - userId,
            )

            is Failure -> oldState
        }

        data object Start : Follow()

        data class Success(val userId: Long) : Follow()

        data class Failure(val error: Throwable) : Follow()
    }
}

@Immutable
data class FollowListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val totalFollowNum: Int = 0,
    val tipsText: String? = null,
    val users: ImmutableList<FollowListBean.FollowUserBean> = persistentListOf(),
    val unfollowedIds: Set<Long> = emptySet(),
) : UiState
