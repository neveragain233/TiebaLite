package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.ui.models.PostData

/**
 * 评论导航方向.
 *
 * 语义基于「可视列表顺序」: [NEXT] 跳转到列表中的下一项(向下滚动),
 * [PREV] 跳转到上一项(向上滚动). 与 [ThreadSortType] 无关, 天然兼容 ASC/DESC.
 */
enum class CommentNavDirection {
    PREV,
    NEXT,
}

/**
 * 楼层数据来源.
 *
 * [DATA] 对应 [ThreadUiState.data](已加载的主楼层列表),
 * [LATEST] 对应 [ThreadUiState.latestPosts](最新回复预览).
 */
enum class PostSource {
    DATA,
    LATEST,
}

/**
 * LazyColumn 里一个 item 槽位的抽象描述.
 *
 * builder([ThreadContent]) 与评论导航(计算 item index)共用同一份 [ThreadListSegment]
 * 序列, 从而消除硬编码偏移(如 `2 + data.size`)并保证两侧顺序一致.
 */
sealed interface ThreadListSegment {

    /** 楼主帖, 含转帖/投票/分隔线. 恒为列表第一个 item(index 0). */
    data object FirstPost : ThreadListSegment

    /** 排序栏(「只看楼主 / 正序 / 倒序」). 可为 stickyHeader 或普通 item. */
    data object Header : ThreadListSegment

    /** 「加载更早」按钮. */
    data object LoadPrevious : ThreadListSegment

    /** 空态提示. */
    data object EmptyTip : ThreadListSegment

    /** ASC 排序下, latestPosts 之前的「以下是最新回复」分隔提示. */
    data object AscTip : ThreadListSegment

    /** DESC 排序下, latestPosts 之后的「以上是最新回复」分隔提示. */
    data object DescTip : ThreadListSegment

    /**
     * 一组连续楼层.
     *
     * @param posts 楼层列表
     * @param source 来源([PostSource.DATA] 或 [PostSource.LATEST])
     */
    data class Posts(val posts: List<PostData>, val source: PostSource) : ThreadListSegment
}

/**
 * 根据 [ThreadUiState] 生成有序的 item 段序列.
 *
 * 该顺序必须与 [ThreadContent] 的渲染分支保持一致; 若修改渲染分支, 必须同步更新此函数
 * (可通过单测对比实际 LazyListScope 顺序来防止不同步).
 */
fun buildThreadListSegments(state: ThreadUiState): List<ThreadListSegment> {
    val latestPosts = state.latestPosts
    val isDesc = state.sortType == ThreadSortType.BY_DESC
    return buildList {
        add(ThreadListSegment.FirstPost)
        if (state.thread != null) {
            add(ThreadListSegment.Header)
        }
        if (isDesc && !latestPosts.isNullOrEmpty()) {
            add(ThreadListSegment.Posts(latestPosts, PostSource.LATEST))
            add(ThreadListSegment.DescTip)
        }
        if (state.pageData.hasPrevious) {
            add(ThreadListSegment.LoadPrevious)
        }
        if (state.data.isEmpty()) {
            add(ThreadListSegment.EmptyTip)
        } else {
            add(ThreadListSegment.Posts(state.data, PostSource.DATA))
        }
        if (!isDesc && !latestPosts.isNullOrEmpty()) {
            add(ThreadListSegment.AscTip)
            add(ThreadListSegment.Posts(latestPosts, PostSource.LATEST))
        }
    }
}

/**
 * 帖子详情页的列表布局模型, 供 builder 与评论导航共享.
 *
 * @param segments 有序 item 段
 * @param firstPostId 楼主帖 id(单独占 index 0, 不计入 [postIndexMap])
 * @param hasPrevious 是否存在更早未加载的楼层
 * @param hasMore 是否存在更晚未加载的楼层
 */
data class ThreadListLayout(
    val segments: List<ThreadListSegment>,
    val firstPostId: Long?,
    val hasPrevious: Boolean,
    val hasMore: Boolean,
) {

    /** 回复(非楼主)楼层 id -> LazyColumn item index. 楼主帖单独处理为 0. */
    val postIndexMap: Map<Long, Int> = buildMap {
        var index = 0
        for (segment in segments) {
            if (segment is ThreadListSegment.Posts) {
                segment.posts.forEach { put(it.id, index++) }
            } else {
                index++
            }
        }
    }

    /** 回复(非楼主)楼层 id, 按显示顺序排列. */
    val orderedReplyPostIds: List<Long> = buildList {
        for (segment in segments) {
            if (segment is ThreadListSegment.Posts) {
                segment.posts.forEach { add(it.id) }
            }
        }
    }

    /** 可导航楼层序列(楼主帖在前, 其余按显示顺序). 仅在无分页间隙时用于相邻判断. */
    val orderedPostIds: List<Long> = buildList {
        firstPostId?.let(::add)
        addAll(orderedReplyPostIds)
    }

    /** LazyColumn 的 item 总数. */
    val totalItems: Int = segments.sumOf { segment ->
        if (segment is ThreadListSegment.Posts) segment.posts.size else 1
    }

    /** 楼层 id -> LazyColumn item index. 楼主帖返回 0. */
    fun itemIndexOf(postId: Long): Int? =
        if (postId == firstPostId) 0 else postIndexMap[postId]

    /**
     * 以 [anchorPostId] 为锚点, 求相邻可导航楼层 id.
     *
     * 返回 null 表示处于边界: 可能需触发加载, 或已到首/末楼.
     * 特别地, 当 [hasPrevious] 为真时, 「上一楼」不会回跳到楼主帖(中间还有未加载楼层),
     * 而是返回 null 以触发加载更早.
     */
    fun targetPostId(anchorPostId: Long, direction: CommentNavDirection): Long? {
        val position = orderedPostIds.indexOf(anchorPostId)
        if (position == -1) return null
        val targetPosition = when (direction) {
            CommentNavDirection.NEXT -> position + 1
            CommentNavDirection.PREV -> position - 1
        }
        val target = orderedPostIds.getOrNull(targetPosition) ?: return null
        // 分页间隙: 上一楼到楼主帖前若还有未加载楼层, 视为边界
        if (direction == CommentNavDirection.PREV && target == firstPostId && hasPrevious) {
            return null
        }
        return target
    }
}

/** 由 [ThreadUiState] 构建 [ThreadListLayout]. */
fun buildThreadListLayout(state: ThreadUiState): ThreadListLayout =
    ThreadListLayout(
        segments = buildThreadListSegments(state),
        firstPostId = state.firstPost?.id,
        hasPrevious = state.pageData.hasPrevious,
        hasMore = state.pageData.hasMore,
    )
