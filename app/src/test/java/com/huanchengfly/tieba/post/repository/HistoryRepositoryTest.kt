package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.UserProfileDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class HistoryRepositoryTest {
    private lateinit var forumHistoryDao: ForumHistoryDao
    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        forumHistoryDao = mockk()
        val database = mockk<TbLiteDatabase> {
            every { threadHistoryDao() } returns mockk<ThreadHistoryDao>()
            every { forumHistoryDao() } returns forumHistoryDao
            every { userProfileDao() } returns mockk<UserProfileDao>()
        }
        repository = HistoryRepository(database)
    }

    @Test
    fun topHistoryUsesAccountFilterWhenUidIsValid() {
        val filtered: Flow<List<ForumHistory>> = flowOf(emptyList())
        every { forumHistoryDao.observeTopNotFollowed(uid = 42L, limit = 10) } returns filtered

        assertSame(filtered, repository.getForumHistoryTop10(followedForumUid = 42L))
        verify(exactly = 1) { forumHistoryDao.observeTopNotFollowed(uid = 42L, limit = 10) }
        verify(exactly = 0) { forumHistoryDao.observeTop(any()) }
    }

    @Test
    fun topHistoryFallsBackToUnfilteredWhenLoggedOut() {
        val unfiltered: Flow<List<ForumHistory>> = flowOf(emptyList())
        every { forumHistoryDao.observeTop(limit = 10) } returns unfiltered

        assertSame(unfiltered, repository.getForumHistoryTop10(followedForumUid = -1L))
        verify(exactly = 1) { forumHistoryDao.observeTop(limit = 10) }
        verify(exactly = 0) { forumHistoryDao.observeTopNotFollowed(any(), any()) }
    }
}
