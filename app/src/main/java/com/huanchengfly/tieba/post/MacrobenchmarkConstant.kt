package com.huanchengfly.tieba.post

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId

// Note: Keep sync with Constants.kt in macrobenchmark
object MacrobenchmarkConstant {

    /**
     * Intent extra: Used to override the [UISettings.reduceEffect] settings.
     *
     * Note: 暗黑模式下为缓解色带会禁用缩放并添加单色杂色, 在中低端设备上性能消耗会明显增加
     * */
    const val EXTRA_REDUCE_EFFECT = "reduce_effect"

    /**
     * Intent extra: Used to control the initial welcome screen state.
     * */
    const val EXTRA_WELCOME_SETUP = "welcome"

    const val TAG_COLUMN = "column"

    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    const val TRACE_ENABLED = BuildConfig.BUILD_TYPE == "benchmarkRelease" || BuildConfig.BUILD_TYPE == "composeTracing"

    const val TRACE_FEED_CARD = "FeedCardTrace"

    const val TRACE_THREAD = "ThreadTrace"

    /**
     * Applies [TAG_COLUMN] to allow modified column to be found in tests.
     *
     * This is a convenience method for a [semantics] that sets [SemanticsPropertyReceiver.testTag].
     */
    fun Modifier.testColumn(): Modifier = this then Modifier.semantics {
        testTagsAsResourceId = true
        testTag = TAG_COLUMN
    }
}