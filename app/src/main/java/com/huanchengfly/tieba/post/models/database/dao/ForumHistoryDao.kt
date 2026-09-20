package com.huanchengfly.tieba.post.models.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.ForumHistory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the forum history table.
 */
@Dao
interface ForumHistoryDao {

    @Upsert
    fun upsert(history: ForumHistory)

    @Query("DELETE FROM forum_history")
    suspend fun deleteAll()

    /**
     * Delete a history record by id.
     *
     * @return the number of history record deleted. This should always be 1.
     */
    @Query("DELETE FROM forum_history WHERE id = :forumId")
    suspend fun deleteById(forumId: Long): Int

    @Query("DELETE FROM forum_history WHERE id in (:ids)")
    suspend fun deleteByIdList(ids: List<Long>): Int

    @Query("SELECT * FROM forum_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeTop(limit: Int = 10): Flow<List<ForumHistory>>

    /**
     * Get forum history paging source.
     * */
    @Query("SELECT * FROM forum_history ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, ForumHistory>

    /**
     * Get forum history excluding forums followed by the current account.
     */
    @Query(
        "SELECT * FROM forum_history history " +
            "WHERE NOT EXISTS (" +
            "SELECT 1 FROM liked_forum liked " +
            "WHERE liked.uid = :uid AND liked.id = history.id" +
            ") ORDER BY history.timestamp DESC"
    )
    fun pagingSourceNotFollowed(uid: Long): PagingSource<Int, ForumHistory>
}
