package com.huanchengfly.tieba.post.utils

import android.os.Trace
import com.huanchengfly.tieba.post.MacrobenchmarkConstant

/**
 * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [label]) and
 * [Trace.endSection].
 *
 * @param label A name of the code section to appear in the trace.
 * @param block A block of code which is being traced.
 */
inline fun <T> trace(label: String, block: () -> T): T {
    return if (!MacrobenchmarkConstant.TRACE_ENABLED) {
        block()
    } else {
        Trace.beginSection(label)

        try {
            block()
        } finally {
            Trace.endSection()
        }
    }
}
