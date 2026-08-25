package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material3.FloatingToolbarState
import androidx.compose.material3.rememberFloatingToolbarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 包装 [FloatingToolbarState]，修复 material3 1.5.0-alpha14 的库缺陷：
 *
 * `FloatingToolbarStateImpl.setOffset`（FloatingToolbar.kt:1476）直接
 * `coerceIn(offsetLimit, 0f)`，但 `ExitAlwaysFloatingToolbarScrollBehavior`
 * 的 `onGloballyPositioned`（FloatingToolbar.kt:726）在布局瞬态下会把
 * `offsetLimit` 算成正数，导致下一次 offset 赋值抛出
 * `IllegalArgumentException: Cannot coerce value to an empty range`。
 *
 * 这里等价实现上游应做的两处修复：offsetLimit 源头钳制到 <= 0，并在 offset
 * 写入前再做一次出口防御。上游合入后可以整体移除本包装。
 */
@Composable
fun rememberSafeFloatingToolbarState(): FloatingToolbarState {
    val delegate = rememberFloatingToolbarState()
    return remember(delegate) { SafeFloatingToolbarState(delegate) }
}

private class SafeFloatingToolbarState(
    private val delegate: FloatingToolbarState,
) : FloatingToolbarState {

    override var offsetLimit: Float
        get() = delegate.offsetLimit.coerceAtMost(0f)
        set(value) {
            delegate.offsetLimit = value.coerceAtMost(0f)
        }

    override var offset: Float
        get() = delegate.offset
        set(value) {
            if (delegate.offsetLimit > 0f) {
                delegate.offsetLimit = 0f
            }
            delegate.offset = value
        }

    override var contentOffset: Float
        get() = delegate.contentOffset
        set(value) {
            delegate.contentOffset = value
        }
}
