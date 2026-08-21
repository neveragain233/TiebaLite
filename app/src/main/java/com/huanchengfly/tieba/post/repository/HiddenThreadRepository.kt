package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.models.database.HiddenThread
import com.huanchengfly.tieba.post.models.database.dao.HiddenThreadDao
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理「隐藏特定帖子」规则的数据仓库.
 *
 * 与 [BlockRepository] 的黑名单 (按吧/关键词/用户规则自动命中) 不同,
 * 隐藏是用户对单个帖子的一次性操作. 隐藏的帖子 id 集合会以 [SharedFlow]
 * 暴露给各 feed 仓库, 用于在列表加载时过滤.
 */
@Singleton
class HiddenThreadRepository @Inject constructor(
    private val localDataSource: HiddenThreadDao,
) {

    /**
     * 当前隐藏的帖子 id 集合, feed 仓库据此过滤.
     */
    val hiddenTids: SharedFlow<Set<Long>> = localDataSource.observeHiddenTids()
        .map { it.toHashSet() }
        .shareInBackground(started = SharingStarted.Lazily)

    /**
     * 观察已隐藏帖子列表 (用于「已隐藏帖子」管理页).
     */
    fun observeHiddenList(): Flow<List<HiddenThread>> = localDataSource.observeHiddenList()

    /**
     * 隐藏一个帖子.
     */
    suspend fun hide(thread: HiddenThread) = withContext(NonCancellable) {
        localDataSource.upsertHidden(thread)
    }

    /**
     * 取消隐藏一个帖子.
     */
    suspend fun unhide(tid: Long) = withContext(NonCancellable) {
        localDataSource.deleteHidden(tid)
    }

    /**
     * 清空所有隐藏帖子.
     */
    suspend fun clearAll() = withContext(NonCancellable) {
        localDataSource.deleteAllHidden()
    }
}
