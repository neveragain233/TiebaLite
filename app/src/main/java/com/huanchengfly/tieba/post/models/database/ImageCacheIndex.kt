package com.huanchengfly.tieba.post.models.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Index of cached image URLs by the thread they were loaded in.
 *
 * Coil's disk cache does not support enumerating entries, so a local index is
 * maintained to enable selectively clearing cache while keeping images of
 * favorited threads. The cache key equals the image URL for string models
 * (Coil's default [coil3.key.StringKeyer]).
 *
 * @param cacheKey Coil disk cache key (the image URL for network images)
 * @param threadId thread the image was loaded in
 * @param timestamp epoch millis of the last record of this entry
 */
@Entity(tableName = "image_cache_index")
data class ImageCacheIndex(
    @PrimaryKey @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    val timestamp: Long,
)
