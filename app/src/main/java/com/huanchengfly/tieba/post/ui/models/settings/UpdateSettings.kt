package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable

/** 自动检查更新的周期, `0` 表示每次打开应用时检查. */
enum class AutoUpdateCheckInterval(val days: Int) {
    EVERY_LAUNCH(0),
    ONE_DAY(1),
    THREE_DAYS(3),
    WEEKLY(7),
    TWO_WEEKS(14),
    MONTHLY(30),
}

/**
 * 更新相关设置
 *
 * @param backgroundDownload 后台下载更新: 开启后自动发现的更新直接进入后台下载
 * @param autoUpdateCheckInterval 自动检查更新的周期
 * @param lastAutoUpdateCheckAt 上次自动检查更新的时间戳
 */
@Immutable
data class UpdateSettings(
    val backgroundDownload: Boolean = true,
    val autoUpdateCheckInterval: AutoUpdateCheckInterval = AutoUpdateCheckInterval.EVERY_LAUNCH,
    val lastAutoUpdateCheckAt: Long = 0L,
)
