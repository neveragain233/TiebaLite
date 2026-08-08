package com.huanchengfly.tieba.macrobenchmark.frames

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.Direction
import com.huanchengfly.tieba.macrobenchmark.EXTRA_REDUCE_EFFECT
import com.huanchengfly.tieba.macrobenchmark.TAG_COLUMN
import com.huanchengfly.tieba.macrobenchmark.TARGET_PACKAGE
import com.huanchengfly.tieba.macrobenchmark.TRACE_FEED_CARD
import com.huanchengfly.tieba.macrobenchmark.startActivityAndSetup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PersonalizedPageFrameTimingBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    private fun measureScrollFeed(intentBuilder: (Intent.() -> Unit)? = null) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                FrameTimingMetric(),
                TraceSectionMetric(TRACE_FEED_CARD, TraceSectionMetric.Mode.Sum),
                TraceSectionMetric("%FeedCard (%", TraceSectionMetric.Mode.Sum),
            ),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.WARM, // restarts activity each iteration
            iterations = 5,
            setupBlock = {
                pressHome(500)
                startActivityAndSetup(welcomeScreen = false, intentBuilder)
                // Waits for an element that corresponds to fully drawn state
                onElement { contentDescription == "动态" && isVisibleToUser }.click()
                device.waitForIdle()
            }
        ) {
            onElement(timeoutMs = 5_000) { viewIdResourceName == TAG_COLUMN }.run {
                setGestureMarginPercentage(0.25f)
                fling(Direction.DOWN)
                fling(Direction.UP)
            }
        }
    }

    /**
     * Note: 暗黑模式下为缓解色带会禁用缩放并添加单色杂色, 在中低端设备上性能消耗会明显增加
     * */
    @Test
    fun scrollFeedCardListFullEffect() = measureScrollFeed(
        intentBuilder = {
            putExtra(EXTRA_REDUCE_EFFECT, false)
        }
    )

    @Test
    fun scrollFeedCardListReducedEffect() = measureScrollFeed(
        intentBuilder = {
            putExtra(EXTRA_REDUCE_EFFECT, true)
        }
    )
}
