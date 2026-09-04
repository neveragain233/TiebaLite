package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material3.FloatingToolbarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * 修复 material3 1.5.0-alpha14 的 [FloatingToolbarState] 布局瞬态缺陷：
 *
 * `FloatingToolbarStateImpl.setOffset` 直接 `coerceIn(offsetLimit, 0f)`，但
 * `ExitAlwaysFloatingToolbarScrollBehavior` 在布局瞬态下可能把 `offsetLimit`
 * 算成正数，导致下一次 offset 赋值抛出 `IllegalArgumentException`。
 *
 * 这里不再复用库里的 `rememberSaveable` 状态：`offset` 与 `offsetLimit` 是依赖
 * 窗口几何和 navigationBars inset 的临时布局状态，跨进程恢复后会与当前几何错位，
 * 造成工具栏只能滑到旧的收起边界。同时通过 [rememberUpdatedState] 保证额外离屏
 * 行程始终读取最新 inset。
 */
@Composable
fun rememberSafeFloatingToolbarState(
    extraExitDistancePx: () -> Float = { 0f },
): FloatingToolbarState {
    val currentExtraExitDistancePx by rememberUpdatedState(extraExitDistancePx)
    return remember {
        SafeFloatingToolbarState(extraExitDistancePx = { currentExtraExitDistancePx() })
    }
}

private class SafeFloatingToolbarState(
    private val extraExitDistancePx: () -> Float,
) : FloatingToolbarState {

    private val offsetLimitState = mutableFloatStateOf(-Float.MAX_VALUE)
    private val offsetState = mutableFloatStateOf(0f)
    private val contentOffsetState = mutableFloatStateOf(0f)

    override var offsetLimit: Float
        get() = offsetLimitState.floatValue
        set(value) {
            // 几何写入值统一追加额外行程, 使工具栏能滑出屏幕而非停在父容器底边
            offsetLimitState.floatValue = (value - extraExitDistancePx()).coerceAtMost(0f)
        }

    override var offset: Float
        get() = offsetState.floatValue
        set(value) {
            if (offsetLimitState.floatValue > 0f) {
                offsetLimitState.floatValue = 0f
            }
            offsetState.floatValue = value.coerceIn(minimumValue = offsetLimit, maximumValue = 0f)
        }

    override var contentOffset: Float
        get() = contentOffsetState.floatValue
        set(value) {
            contentOffsetState.floatValue = value
        }
}
