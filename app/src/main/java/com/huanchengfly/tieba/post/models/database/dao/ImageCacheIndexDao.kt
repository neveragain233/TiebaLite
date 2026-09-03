package com.huanchengfly.tieba.post.models.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.huanchengfly.tieba.post.models.database.ImageCacheIndex

@Dao
interface ImageCacheIndexDao {

    @Upsert
    suspend fun upsertAll(entries: List<ImageCacheIndex>)

    @Query("SELECT * FROM image_cache_index")
    suspend fun getAll(): List<ImageCacheIndex>

    @Query("SELECT COUNT(*) FROM image_cache_index")
    suspend fun count(): Int

    @Query("DELETE FROM image_cache_index WHERE cache_key IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>)

    @Query("DELETE FROM image_cache_index WHERE cache_key IN (SELECT cache_key FROM image_cache_index ORDER BY timestamp ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int)
}
