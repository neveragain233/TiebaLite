package com.huanchengfly.tieba.post.models.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 隐藏的帖子 (主题帖) 规则
 *
 * 与黑名单 (按吧/关键词/用户自动命中) 不同, 隐藏是用户对单个帖子的
 * 一次性操作, 命中后帖子不再出现在 feed 列表中, 但通过链接/历史直接
 * 打开详情页不受影响.
 *
 * @param tid 帖子 id
 * @param forumName 吧名
 * @param title 帖子标题 (用于已隐藏列表展示)
 * @param authorName 楼主昵称 (用于已隐藏列表展示, 可选)
 * @param hiddenTime 隐藏时间戳
 */
@Entity(tableName = "hidden_thread")
data class HiddenThread(
    @PrimaryKey
    val tid: Long,
    val forumName: String,
    val title: String,
    val authorName: String?,
    val hiddenTime: Long,
)
