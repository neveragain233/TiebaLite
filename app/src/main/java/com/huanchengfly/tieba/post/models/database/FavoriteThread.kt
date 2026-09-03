package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local snapshot of the user's favorited (collected) threads on the server side.
 *
 * Favorites list is server-side only, so a local snapshot is maintained for offline
 * scenarios such as "clear image cache on launch but keep favorited threads' images".
 *
 * @param id thread ID
 * @param timestamp epoch millis of the last sync of this entry
 */
@Entity(tableName = "favorite_threads")
data class FavoriteThread(
    @PrimaryKey val id: Long,
    val timestamp: Long,
)
