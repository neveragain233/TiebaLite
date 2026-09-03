package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.FavoriteThread

@Dao
interface FavoriteThreadDao {

    @Upsert
    suspend fun upsertAll(favorites: List<FavoriteThread>)

    @Query("DELETE FROM favorite_threads WHERE id = :threadId")
    suspend fun deleteById(threadId: Long)

    @Query("SELECT id FROM favorite_threads")
    suspend fun getAllIds(): List<Long>
}
