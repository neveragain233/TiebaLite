package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable

/**
 * 更新相关设置
 *
 * @param backgroundDownload 后台下载更新: 开启后下载转入后台并在通知栏提示安装
 */
@Immutable
data class UpdateSettings(
    val backgroundDownload: Boolean = true,
)
