package com.huanchengfly.tieba.post.components.coil

import android.content.Context
import coil3.Extras
import coil3.intercept.Interceptor
import coil3.intercept.Interceptor.Chain
import coil3.request.ImageResult

/**
 * Records which thread an image request belongs to, so the image cache index
 * can be maintained for "clear image cache on launch" with a
 * "keep favorited threads' images" exception.
 *
 * Only string (URL) models with a [LocalCurrentThreadId]-sourced `thread_id`
 * request parameter are indexed; the Coil disk cache key for string models is
 * the URL string itself, so the recorded key matches the disk cache entry key.
 */
/**
 * Extras key marking the thread an image request belongs to.
 *
 * Uses [Extras] (Coil 3 replaced Coil 2's request parameters with typed extras).
 */
val THREAD_ID_EXTRA = Extras.Key<Long?>(null)

class ThreadImageCacheInterceptor(private val context: Context) : Interceptor {

    override suspend fun intercept(chain: Chain): ImageResult {
        val request = chain.request
        val threadId = request.extras.get(THREAD_ID_EXTRA)
        if (threadId != null && request.data is String) {
            ImageCacheIndexRecorder.record(context, request.data as String, threadId)
        }
        return chain.proceed()
    }
}
