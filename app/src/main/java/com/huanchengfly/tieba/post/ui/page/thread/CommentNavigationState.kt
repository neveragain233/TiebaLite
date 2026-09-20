package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** Owns navigation scrolls so a cancelled older scroll cannot clear a newer one's state. */
internal class CommentNavigationScrollState {
    var isScrolling by mutableStateOf(false)
        private set
    private var job: Job? = null
    private var generation = 0

    fun cancel() {
        generation++
        job?.cancel()
        isScrolling = false
    }

    fun launch(
        scope: CoroutineScope,
        onCancelled: () -> Unit,
        onArrived: () -> Unit = {},
        scroll: suspend () -> Unit,
    ) {
        cancel()
        val token = generation
        isScrolling = true
        job = scope.launch {
            try {
                scroll()
                ensureActive()
                if (generation == token) onArrived()
            } catch (e: CancellationException) {
                if (generation == token) onCancelled()
                throw e
            } finally {
                if (generation == token) isScrolling = false
            }
        }
    }
}

/** Shared by single/dual buttons; feedback follows successful NEXT navigation, not button icons. */
internal class CommentNavigationEndFeedback {
    private var lastEndId: Long? = null

    fun reset() { lastEndId = null }

    fun onArrived(
        direction: CommentNavDirection,
        targetId: Long,
        orderedPostIds: List<Long>,
        hasMore: Boolean,
        atListBottom: Boolean,
        lastPostVisible: Boolean,
    ): Boolean {
        val endId = orderedPostIds.lastOrNull()
        if (direction != CommentNavDirection.NEXT) {
            if (targetId != endId) reset()
            return false
        }
        val reachedEnd = !hasMore && endId != null &&
            (targetId == endId || (atListBottom && lastPostVisible))
        if (!reachedEnd) reset()
        if (!reachedEnd || lastEndId == endId) return false
        lastEndId = endId
        return true
    }
}
