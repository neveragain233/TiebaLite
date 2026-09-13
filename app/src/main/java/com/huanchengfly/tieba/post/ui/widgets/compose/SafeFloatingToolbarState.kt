package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material3.FloatingToolbarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect

/**
 * 修复 material3 1.5.0-alpha14 的 [FloatingToolbarState] 布局瞬态缺陷：
 *
 * `FloatingToolbarStateImpl.setOffset` 直接 `coerceIn(offsetLimit, 0f)`，但
 * `ExitAlwaysFloatingToolbarScrollBehavior` 在布局瞬态下可能把 `offsetLimit`
 * 算成正数，导致下一次 offset 赋值抛出 `IllegalArgumentException`。
 *
 * 这里不再复用库里的 `rememberSaveable` 状态：`offset` 与 `offsetLimit` 是依赖
 * 窗口几何和 navigationBars inset 的临时布局状态，跨进程恢复后会与当前几何错位，
 * 造成工具栏只能滑到旧的收起边界。同时在 inset 更新后重算隐藏边界，
 * 并同步已隐藏的位移。
 */
@Composable
fun rememberSafeFloatingToolbarState(
    extraExitDistancePx: () -> Float = { 0f },
): FloatingToolbarState {
    val extraExitPx = extraExitDistancePx()
    val state = remember { SafeFloatingToolbarState() }
    SideEffect { state.updateExtraExitDistance(extraExitPx) }
    return state
}

internal class SafeFloatingToolbarState : FloatingToolbarState {
    private var measuredOffsetLimit: Float? = null
    private var extraExitDistance = 0f

    fun updateExtraExitDistance(value: Float) {
        if (extraExitDistance == value) return
        extraExitDistance = value
        measuredOffsetLimit?.let(::updateOffsetLimit)
    }

    private fun updateOffsetLimit(measured: Float) {
        val previousLimit = offsetLimitState.floatValue
        val wasHidden = previousLimit < 0f && previousLimit != -Float.MAX_VALUE &&
            offsetState.floatValue <= previousLimit
        val newLimit = (measured - extraExitDistance).coerceAtMost(0f)
        offsetLimitState.floatValue = newLimit
        // 恢复/重布局后仍贴住新的隐藏边界，避免旧位移留下残影。
        offsetState.floatValue = if (wasHidden) newLimit else offsetState.floatValue.coerceIn(newLimit, 0f)
    }

    private val offsetLimitState = mutableFloatStateOf(-Float.MAX_VALUE)
    private val offsetState = mutableFloatStateOf(0f)
    private val contentOffsetState = mutableFloatStateOf(0f)

    override var offsetLimit: Float
        get() = offsetLimitState.floatValue
        set(value) {
            // 几何写入值统一追加额外行程, 使工具栏能滑出屏幕而非停在父容器底边
            measuredOffsetLimit = value
            updateOffsetLimit(value)
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
