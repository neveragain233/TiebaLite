package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import com.huanchengfly.tieba.post.models.database.HiddenThread
import com.huanchengfly.tieba.post.repository.HiddenThreadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class HiddenPostListViewModel @Inject constructor(
    private val hiddenRepo: HiddenThreadRepository,
) : BaseBlockListViewModel<HiddenThread>() {

    override val _blackList: Flow<List<HiddenThread>?> = hiddenRepo.observeHiddenList()

    override val _whiteList: Flow<List<HiddenThread>?> = flowOf(null)

    override suspend fun upsertInternal(item: HiddenThread) = Unit

    override suspend fun deleteInternal(item: HiddenThread) {
        hiddenRepo.unhide(item.tid)
    }

    override suspend fun deleteListInternal(items: List<HiddenThread>) {
        items.forEach { hiddenRepo.unhide(it.tid) }
    }
}
