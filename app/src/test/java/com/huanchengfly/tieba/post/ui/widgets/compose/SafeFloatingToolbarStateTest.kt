package com.huanchengfly.tieba.post.ui.widgets.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeFloatingToolbarStateTest {
    @Test fun hiddenOffsetFollowsRestoredGeometryAndInsets() {
        val state = SafeFloatingToolbarState()
        state.offsetLimit = -48f
        state.offset = -48f
        state.updateExtraExitDistance(32f)
        assertEquals(-80f, state.offset, 0f)
        state.offsetLimit = -60f
        assertEquals(-92f, state.offset, 0f)
        state.updateExtraExitDistance(0f)
        assertEquals(-60f, state.offset, 0f)
    }

    @Test fun visibleAndPartialOffsetsStayValidAfterRemeasure() {
        val state = SafeFloatingToolbarState()
        state.offsetLimit = -100f
        assertEquals(0f, state.offset, 0f)
        state.offset = -70f
        state.offsetLimit = -48f
        assertEquals(-48f, state.offset, 0f)
        state.updateExtraExitDistance(20f)
        assertEquals(-68f, state.offset, 0f)
    }

    @Test fun transientPositiveLimitDoesNotCrashOrHideInitialState() {
        val state = SafeFloatingToolbarState()
        state.offsetLimit = 10f
        state.offset = -20f
        assertEquals(0f, state.offset, 0f)
        state.offsetLimit = -48f
        assertEquals(0f, state.offset, 0f)
    }
}
