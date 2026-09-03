package com.huanchengfly.tieba.post.components.coil

import android.content.Context
import com.huanchengfly.tieba.post.models.database.ImageCacheIndex
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.ImageCacheIndexDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Records (image URL -> thread id) pairs for images loaded inside a thread page,
 * so that [com.huanchengfly.tieba.post.utils.ImageCacheUtil] can selectively
 * clear the Coil disk cache while keeping favorited threads' images.
 *
 * Writes are buffered and flushed periodically to avoid a DB write per image.
 */
object ImageCacheIndexRecorder {

    private const val MAX_INDEX_SIZE = 20_000
    private const val FLUSH_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<String, ImageCacheIndex>()

    @Volatile
    private var flushLoopStarted = false

    /**
     * @param url image URL, which is also the Coil disk cache key for string models
     */
    fun record(context: Context, url: String, threadId: Long) {
        val dao = TbLiteDatabase.getInstance(context).imageCacheIndexDao()
        pending[url] = ImageCacheIndex(
            cacheKey = url,
            threadId = threadId,
            timestamp = System.currentTimeMillis(),
        )
        startFlushLoopIfNeeded(dao)
    }

    private fun startFlushLoopIfNeeded(dao: ImageCacheIndexDao) {
        if (flushLoopStarted) return
        synchronized(this) {
            if (flushLoopStarted) return
            flushLoopStarted = true
        }
        scope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                runCatching { flush(dao) }
            }
        }
    }

    suspend fun flush(dao: ImageCacheIndexDao) {
        if (pending.isEmpty()) return
        val batch = ArrayList<ImageCacheIndex>(pending.size)
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            batch.add(iterator.next().value)
            iterator.remove()
        }
        dao.upsertAll(batch)
        val count = dao.count()
        if (count > MAX_INDEX_SIZE) {
            dao.deleteOldest(count - MAX_INDEX_SIZE)
        }
    }
}
