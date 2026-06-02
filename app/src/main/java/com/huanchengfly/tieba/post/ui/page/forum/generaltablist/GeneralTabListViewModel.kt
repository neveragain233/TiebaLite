package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.ThreadItemList
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Stable
@HiltViewModel
class GeneralTabListViewModel @Inject constructor(
    private val forumRepo: ForumRepository,
    settingsRepo: SettingsRepository,
) : BaseViewModel<GeneralTabListUiIntent, GeneralTabListPartialChange, GeneralTabListUiState, GeneralTabListUiEvent>() {

    val hideBlocked: StateFlow<Boolean> = settingsRepo.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = true)

    override fun createInitialState(): GeneralTabListUiState = GeneralTabListUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<GeneralTabListUiIntent, GeneralTabListPartialChange, GeneralTabListUiState> =
        GeneralTabListPartialChangeProducer(forumRepo)

    override fun dispatchEvent(partialChange: GeneralTabListPartialChange): UiEvent? =
        when (partialChange) {
            is GeneralTabListPartialChange.FirstLoad.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is GeneralTabListPartialChange.Refresh.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is GeneralTabListPartialChange.LoadMore.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is GeneralTabListPartialChange.Agree.Failure -> {
                GeneralTabListUiEvent.AgreeFail(
                    partialChange.threadId,
                    partialChange.postId,
                    partialChange.hasAgree,
                    partialChange.error.getErrorCode(),
                    partialChange.error.getErrorMessage()
                )
            }
            else -> null
        }
}

private class GeneralTabListPartialChangeProducer(private val forumRepo: ForumRepository) :
    PartialChangeProducer<GeneralTabListUiIntent, GeneralTabListPartialChange, GeneralTabListUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<GeneralTabListUiIntent>): Flow<GeneralTabListPartialChange> =
        merge(
            intentFlow.filterIsInstance<GeneralTabListUiIntent.FirstLoad>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<GeneralTabListUiIntent.Refresh>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<GeneralTabListUiIntent.LoadMore>()
                .flatMapConcat { it.producePartialChange() },
            intentFlow.filterIsInstance<GeneralTabListUiIntent.Agree>()
                .flatMapConcat { it.producePartialChange() },
        )

    private fun GeneralTabListUiIntent.FirstLoad.producePartialChange() =
        forumRepo.generalTabList(
            forumId = forumId,
            forumName = forumName,
            tabId = navTabInfo.tabId,
            tabType = navTabInfo.tabType,
            tabName = navTabInfo.tabName,
            isGeneralTab = navTabInfo.isGeneralTab,
            pn = 1,
            sortType = this.sortType,
            lastThreadId = 0,
            isDefaultNavTab = navTabInfo.isDefault,
        ).map<ThreadItemList, GeneralTabListPartialChange.FirstLoad> { response ->
            val threadList = response.threads
            GeneralTabListPartialChange.FirstLoad.Success(
                threadList = threadList,
                hasMore = response.hasMore,
                lastThreadId = threadList.lastOrNull()?.id ?: 0,
                sortType = sortType,
            )
        }
            .onStart { emit(GeneralTabListPartialChange.FirstLoad.Start) }
            .catch { emit(GeneralTabListPartialChange.FirstLoad.Failure(it)) }

    private fun GeneralTabListUiIntent.Refresh.producePartialChange() =
        forumRepo.generalTabList(
            forumId = forumId,
            forumName = forumName,
            tabId = navTabInfo.tabId,
            tabType = navTabInfo.tabType,
            tabName = navTabInfo.tabName,
            isGeneralTab = navTabInfo.isGeneralTab,
            pn = 1,
            sortType = this.sortType,
            lastThreadId = 0,
            isDefaultNavTab = navTabInfo.isDefault,
        ).map<ThreadItemList, GeneralTabListPartialChange.Refresh> { response ->
            val threadList = response.threads
            GeneralTabListPartialChange.Refresh.Success(
                threadList = threadList,
                hasMore = response.hasMore,
                lastThreadId = threadList.lastOrNull()?.id ?: 0,
                sortType = sortType,
            )
        }
            .onStart { emit(GeneralTabListPartialChange.Refresh.Start) }
            .catch { emit(GeneralTabListPartialChange.Refresh.Failure(it)) }

    private fun GeneralTabListUiIntent.LoadMore.producePartialChange() =
        forumRepo.generalTabList(
            forumId = forumId,
            forumName = forumName,
            tabId = navTabInfo.tabId,
            tabType = navTabInfo.tabType,
            tabName = navTabInfo.tabName,
            isGeneralTab = navTabInfo.isGeneralTab,
            pn = currentPage + 1,
            sortType = this.sortType,
            lastThreadId = lastThreadId,
            isDefaultNavTab = navTabInfo.isDefault,
        ).map<ThreadItemList, GeneralTabListPartialChange.LoadMore> { response ->
            val threadList = response.threads
            GeneralTabListPartialChange.LoadMore.Success(
                threadList = threadList,
                hasMore = response.hasMore && threadList.isNotEmpty(),
                currentPage = currentPage + 1,
                lastThreadId = threadList.lastOrNull()?.id ?: lastThreadId,
            )
        }
            .onStart { emit(GeneralTabListPartialChange.LoadMore.Start) }
            .catch { emit(GeneralTabListPartialChange.LoadMore.Failure(it)) }

    private fun GeneralTabListUiIntent.Agree.producePartialChange(): Flow<GeneralTabListPartialChange.Agree> =
        TiebaApi.getInstance().opAgreeFlow(
            threadId.toString(),
            postId.toString(),
            hasAgree,
            objType = 3
        ).map<AgreeBean, GeneralTabListPartialChange.Agree> {
            GeneralTabListPartialChange.Agree.Success(
                threadId,
                hasAgree xor 1
            )
        }
            .catch {
                emit(
                    GeneralTabListPartialChange.Agree.Failure(
                        threadId,
                        postId,
                        hasAgree,
                        it
                    )
                )
            }
            .onStart { emit(GeneralTabListPartialChange.Agree.Start(threadId, hasAgree xor 1)) }
}

sealed interface GeneralTabListUiIntent : UiIntent {
    data class FirstLoad(
        val forumId: Long,
        val forumName: String,
        val navTabInfo: FrsTabInfo,
        val sortType: Int = -1,
    ) : GeneralTabListUiIntent

    data class Refresh(
        val forumId: Long,
        val forumName: String,
        val navTabInfo: FrsTabInfo,
        val sortType: Int = -1,
    ) : GeneralTabListUiIntent

    data class LoadMore(
        val forumId: Long,
        val forumName: String,
        val navTabInfo: FrsTabInfo,
        val currentPage: Int,
        val lastThreadId: Long,
        val sortType: Int = -1,
    ) : GeneralTabListUiIntent

    data class Agree(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Int
    ) : GeneralTabListUiIntent
}

sealed interface GeneralTabListPartialChange : PartialChange<GeneralTabListUiState> {
    sealed class FirstLoad : GeneralTabListPartialChange {
        override fun reduce(oldState: GeneralTabListUiState): GeneralTabListUiState = when (this) {
            Start -> oldState.copy(isRefreshing = true)
            is Success -> oldState.copy(
                isRefreshing = false,
                threadList = runBlocking { threadList.distinctById() },
                hasMore = hasMore,
                currentPage = 1,
                lastThreadId = lastThreadId,
                sortType = sortType,
            )
            is Failure -> oldState.copy(isRefreshing = false)
        }

        data object Start : FirstLoad()
        data class Success(
            val threadList: List<ThreadItem>,
            val hasMore: Boolean,
            val lastThreadId: Long,
            val sortType: Int = -1,
        ) : FirstLoad()
        data class Failure(val error: Throwable) : FirstLoad()
    }

    sealed class Refresh : GeneralTabListPartialChange {
        override fun reduce(oldState: GeneralTabListUiState): GeneralTabListUiState = when (this) {
            Start -> oldState.copy(isRefreshing = true)
            is Success -> oldState.copy(
                isRefreshing = false,
                threadList = runBlocking { threadList.distinctById() },
                hasMore = hasMore,
                currentPage = 1,
                lastThreadId = lastThreadId,
                sortType = sortType,
            )
            is Failure -> oldState.copy(isRefreshing = false)
        }

        data object Start : Refresh()
        data class Success(
            val threadList: List<ThreadItem>,
            val hasMore: Boolean,
            val lastThreadId: Long,
            val sortType: Int = -1,
        ) : Refresh()
        data class Failure(val error: Throwable) : Refresh()
    }

    sealed class LoadMore : GeneralTabListPartialChange {
        override fun reduce(oldState: GeneralTabListUiState): GeneralTabListUiState = when (this) {
            Start -> oldState.copy(isLoadingMore = true)
            is Success -> oldState.copy(
                isLoadingMore = false,
                threadList = runBlocking { (oldState.threadList + threadList).distinctById() },
                hasMore = hasMore,
                currentPage = currentPage,
                lastThreadId = lastThreadId,
            )
            is Failure -> oldState.copy(isLoadingMore = false)
        }

        data object Start : LoadMore()
        data class Success(
            val threadList: List<ThreadItem>,
            val hasMore: Boolean,
            val currentPage: Int,
            val lastThreadId: Long,
        ) : LoadMore()
        data class Failure(val error: Throwable) : LoadMore()
    }

    sealed class Agree private constructor() : GeneralTabListPartialChange {
        private fun List<ThreadItem>.updateAgreeStatus(
            threadId: Long,
            hasAgree: Int,
        ): List<ThreadItem> {
            return runBlocking {
                updateLikeStatus(threadId, hasAgree == 1, false)
            }
        }

        override fun reduce(oldState: GeneralTabListUiState): GeneralTabListUiState =
            when (this) {
                is Start -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Success -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }

                is Failure -> {
                    oldState.copy(
                        threadList = oldState.threadList.updateAgreeStatus(
                            threadId,
                            hasAgree
                        )
                    )
                }
            }

        data class Start(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Success(
            val threadId: Long,
            val hasAgree: Int
        ) : Agree()

        data class Failure(
            val threadId: Long,
            val postId: Long,
            val hasAgree: Int,
            val error: Throwable
        ) : Agree()
    }
}

data class GeneralTabListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val threadList: List<ThreadItem> = persistentListOf(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val lastThreadId: Long = 0,
    val sortType: Int = -1,
) : UiState

sealed interface GeneralTabListUiEvent : UiEvent {
    data object BackToTop : GeneralTabListUiEvent
    data class Refresh(val sortType: Int = -1) : GeneralTabListUiEvent

    data class AgreeFail(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Int,
        val errorCode: Int,
        val errorMsg: String
    ) : GeneralTabListUiEvent
}
