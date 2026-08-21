package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.HiddenThread
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [hidden_thread] table.
 */
@Dao
interface HiddenThreadDao {

    /**
     * Insert or update a hidden thread rule. If a rule already exists, update it.
     */
    @Upsert
    suspend fun upsertHidden(thread: HiddenThread)

    /**
     * Insert one or more hidden thread rules. If a rule already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHidden(vararg threads: HiddenThread)

    @Query("DELETE FROM hidden_thread WHERE tid = :tid")
    suspend fun deleteHidden(tid: Long): Int

    @Query("DELETE FROM hidden_thread")
    suspend fun deleteAllHidden()

    /**
     * Observes list of hidden thread rules, newest first.
     */
    @Query("SELECT * FROM hidden_thread ORDER BY hiddenTime DESC")
    fun observeHiddenList(): Flow<List<HiddenThread>>

    /**
     * Observes the set of hidden thread ids, used to filter feed lists.
     */
    @Query("SELECT tid FROM hidden_thread")
    fun observeHiddenTids(): Flow<List<Long>>

    /**
     * Select all hidden thread rules for backup.
     */
    @Query("SELECT * FROM hidden_thread ORDER BY hiddenTime")
    suspend fun getAllHidden(): List<HiddenThread>
}
