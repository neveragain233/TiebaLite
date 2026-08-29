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
 *
 * [extraExitDistancePx] 在缺陷修复之外追加额外的收起行程(px)：M3 的
 * `floatingScrollBehavior` 按「滑到父容器底边」计算行程，工具栏滑完自身高度后
 * 仍停留在导航栏 inset 区域内；追加 inset 距离可让工具栏完全离屏。每次布局
 * M3 都会按几何重写 offsetLimit，包装层在写入时统一追加该距离。
 */
@Composable
fun rememberSafeFloatingToolbarState(
    extraExitDistancePx: () -> Float = { 0f },
): FloatingToolbarState {
    val delegate = rememberFloatingToolbarState()
    return remember(delegate) { SafeFloatingToolbarState(delegate, extraExitDistancePx) }
}

private class SafeFloatingToolbarState(
    private val delegate: FloatingToolbarState,
    private val extraExitDistancePx: () -> Float,
) : FloatingToolbarState {

    override var offsetLimit: Float
        get() = delegate.offsetLimit
        set(value) {
            // 几何写入值统一追加额外行程, 使工具栏能滑出屏幕而非停在父容器底边
            delegate.offsetLimit = (value - extraExitDistancePx()).coerceAtMost(0f)
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
