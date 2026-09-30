package com.huanchengfly.tieba.post.arch

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil.applyNavigationBarAppearance

abstract class BaseComposeActivity : BaseActivity() {

    protected val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    /**
     * 配置变化版本号. [onConfigurationChanged] 时自增, 供 Compose 侧强制重算
     * 窗口自适应信息, 避免折叠/旋转后窗口容器尺寸未刷新导致布局卡在旧方向.
     */
    var configVersion by mutableStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ThemeUtil.onUpdateSystemUiMode(this)

        setContent {
            val uiSettings by App.INSTANCE.settingRepository.uiSettings
                .collectAsStateWithLifecycle(initialValue = UISettings())
            StrongBox {
                val colorState by ThemeUtil.colorState
                val colorScheme = colorState.colorScheme

                LaunchedEffect(colorScheme, uiSettings.immersiveWhiteNavigationBar) {
                    windowInsetsController.setAppearanceLightStatusBars(ThemeUtil.isStatusBarFontDark(colorScheme))
                    window.applyNavigationBarAppearance(
                        controller = windowInsetsController,
                        colorScheme = colorScheme,
                        immersiveWhiteNavigationBar = uiSettings.immersiveWhiteNavigationBar,
                    )
                }
            }
            CompositionLocalProvider(LocalUISettings provides uiSettings) {
                Content()
            }
        }
    }

    @Composable
    abstract fun Content()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Update night theme if needed
        ThemeUtil.onUpdateSystemUiMode(this)
        configVersion++
    }

    fun handleCommonEvent(event: CommonUiEvent) {
        when (event) {
            is CommonUiEvent.Toast -> {
                Toast.makeText(this, event.message, event.length).show()
            }

            else -> {}
        }
    }
}

sealed interface CommonUiEvent : UiEvent {

    object FeatureUnavailable : CommonUiEvent

    object NavigateUp : CommonUiEvent

    class ToastError(override val message: CharSequence, val code: Int): Toast(message) {
        constructor(e: Throwable) : this(message = e.getErrorMessage(), code = e.getErrorCode())
    }

    open class Toast(
        open val message: CharSequence,
        val length: Int = android.widget.Toast.LENGTH_SHORT
    ) : CommonUiEvent
}
