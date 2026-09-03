package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模拟「下键」在站点序列里的落点: 按实测视口反查已走完的最后一站, 返回下一站偏移;
 * 站点走完(含恰好停在最后一站)时返回 null, 表示该跳楼了.
 */
private fun nextWaypointOffset(positions: List<Int>, scrolledPastPx: Int): Int? {
    val step = resolvedWaypointIndex(positions, scrolledPastPx, NavWaypointToleranceTestPx)
    return positions.getOrNull(step + 1)
}

private const val NavWaypointToleranceTestPx = 24

private fun post(id: Long): PostData = PostData(
    id = id,
    author = UserData(
        id = id,
        name = "user$id",
        nameShow = "user$id",
        showBothName = false,
        avatarUrl = "",
        portrait = "",
        ip = "",
        levelId = 0,
        bawuType = null,
        isLz = false,
    ),
    floor = id.toInt(),
    title = null,
    time = 0,
    like = Like(liked = false, count = 0),
    blocked = false,
    plainText = "",
    contentRenders = emptyList(),
    subPosts = null,
    subPostNumber = 0,
)

private fun state(
    data: List<PostData>,
    firstPost: PostData? = post(1),
    latestPosts: List<PostData>? = null,
    sortType: Int = ThreadSortType.BY_ASC,
    hasPrevious: Boolean = false,
    hasMore: Boolean = false,
) = ThreadUiState(
    data = data,
    firstPost = firstPost,
    latestPosts = latestPosts,
    sortType = sortType,
    thread = ThreadInfoData(
        id = firstPost?.id ?: 1,
        title = "title",
        collectMarkPid = null,
        firstPostId = firstPost?.id ?: 1,
        like = Like(liked = false, count = 0),
        originThreadInfo = null,
        replyNum = data.size,
        simpleForum = Triple(1L, "forum", null),
        pollInfo = null,
    ),
    pageData = PageData(hasPrevious = hasPrevious, hasMore = hasMore),
)

private fun List<ThreadListSegment>.describe(): List<String> = map { segment ->
    when (segment) {
        is ThreadListSegment.Posts -> "${segment.source}:${segment.posts.map { it.id }}"
        ThreadListSegment.FirstPost -> "FirstPost"
        ThreadListSegment.Header -> "Header"
        ThreadListSegment.LoadPrevious -> "LoadPrevious"
        ThreadListSegment.EmptyTip -> "EmptyTip"
        ThreadListSegment.AscTip -> "AscTip"
        ThreadListSegment.DescTip -> "DescTip"
    }
}

class ThreadListLayoutTest {

    @Test
    fun ascLayout_ordersSegmentsAndMapsIndices() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(2), post(3), post(4)),
                firstPost = post(1),
            )
        )

        assertEquals(
            listOf("FirstPost", "Header", "DATA:[2, 3, 4]"),
            layout.segments.describe(),
        )
        assertEquals(mapOf(2L to 2, 3L to 3, 4L to 4), layout.postIndexMap)
        assertEquals(listOf(1L, 2L, 3L, 4L), layout.orderedPostIds)
        assertEquals(0, layout.itemIndexOf(1L))
        assertEquals(2, layout.itemIndexOf(2L))
        assertEquals(3L, layout.targetPostId(2L, CommentNavDirection.NEXT))
        assertEquals(1L, layout.targetPostId(2L, CommentNavDirection.PREV))
        assertNull(layout.targetPostId(4L, CommentNavDirection.NEXT))
    }

    @Test
    fun descWithLatest_placesLatestAtTopAndMapsIndices() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(9), post(8), post(7)),
                firstPost = post(1),
                latestPosts = listOf(post(10), post(11)),
                sortType = ThreadSortType.BY_DESC,
            )
        )

        assertEquals(
            listOf("FirstPost", "Header", "LATEST:[10, 11]", "DescTip", "DATA:[9, 8, 7]"),
            layout.segments.describe(),
        )
        assertEquals(listOf(1L, 10L, 11L, 9L, 8L, 7L), layout.orderedPostIds)
        assertEquals(9L, layout.targetPostId(11L, CommentNavDirection.NEXT))
        assertEquals(11L, layout.targetPostId(9L, CommentNavDirection.PREV))
        assertNull(layout.targetPostId(7L, CommentNavDirection.NEXT))
    }

    @Test
    fun gapPrevious_doesNotJumpToOpWhenHasPrevious() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(50), post(51)),
                firstPost = post(1),
                hasPrevious = true,
            )
        )

        // 中间还有未加载楼层, 上一楼不应回跳楼主帖
        assertNull(layout.targetPostId(50L, CommentNavDirection.PREV))
        // 楼主帖的下一楼仍是第一加载楼
        assertEquals(50L, layout.targetPostId(1L, CommentNavDirection.NEXT))
    }

    @Test
    fun noPrevious_firstReplyPrevGoesToOp() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(2), post(3)),
                firstPost = post(1),
                hasPrevious = false,
            )
        )

        assertEquals(1L, layout.targetPostId(2L, CommentNavDirection.PREV))
        assertEquals(0, layout.itemIndexOf(1L))
    }

    @Test
    fun emptyData_noNavigationTargets() {
        val layout = buildThreadListLayout(
            state(
                data = emptyList(),
                firstPost = post(1),
            )
        )

        assertEquals(listOf(1L), layout.orderedPostIds)
        assertNull(layout.targetPostId(1L, CommentNavDirection.NEXT))
        assertNull(layout.targetPostId(1L, CommentNavDirection.PREV))
        assertEquals(0, layout.itemIndexOf(1L))
    }
    @Test
    fun endHapticTarget_onlyForNextToLastReplyWithoutMore() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(2), post(3)),
                firstPost = post(1),
            )
        )

        assertEquals(3L, resolveEndHapticTarget(
            direction = CommentNavDirection.NEXT,
            targetPostId = 3L,
            orderedPostIds = layout.orderedPostIds,
            hasMore = false,
        ))
        assertNull(resolveEndHapticTarget(
            direction = CommentNavDirection.PREV,
            targetPostId = 3L,
            orderedPostIds = layout.orderedPostIds,
            hasMore = false,
        ))
        assertNull(resolveEndHapticTarget(
            direction = CommentNavDirection.NEXT,
            targetPostId = 2L,
            orderedPostIds = layout.orderedPostIds,
            hasMore = false,
        ))
    }

    @Test
    fun endHapticTarget_skippedWhenMorePagesRemain() {
        val layout = buildThreadListLayout(
            state(
                data = listOf(post(2), post(3)),
                firstPost = post(1),
                hasMore = true,
            )
        )

        assertNull(resolveEndHapticTarget(
            direction = CommentNavDirection.NEXT,
            targetPostId = 3L,
            orderedPostIds = layout.orderedPostIds,
            hasMore = true,
        ))
    }

    @Test
    fun waypointIndex_walksSitesAlreadyBehindViewport() {
        val positions = listOf(0, 100, 200, 300)

        // 展开长图后视口停在楼内 250px: 楼顶/图1/图2 都已走过, 进度应为下标 2,
        // 下键下一站是收起按钮(下标 3) —— 而不是旧逻辑那样先空走一步没有动作
        assertEquals(2, resolvedWaypointIndex(positions, scrolledPastPx = 250, tolerancePx = 24))
        assertEquals(300, nextWaypointOffset(positions, scrolledPastPx = 250))
    }

    @Test
    fun waypointIndex_skipsSiteGluedToPostTop() {
        // 首张图紧贴楼顶, 两个站点落在容差内: 不能被当成「还没走过」而原地不动
        val positions = listOf(0, 5, 300)

        assertEquals(1, resolvedWaypointIndex(positions, scrolledPastPx = 0, tolerancePx = 24))
        assertEquals(300, nextWaypointOffset(positions, scrolledPastPx = 0))
    }

    @Test
    fun waypointIndex_countsSiteAlignedUnderSortBar() {
        // 站点滚到置顶排序栏下沿时, 楼头实际比基准线多滚出一段排序栏高度(此处 94px).
        // 反查用同一条基准线, 该站才算「已到达」, 再按 ▼ 直接去下一站而不是重对齐
        val positions = listOf(0, 100, 200, 300)

        assertEquals(
            2,
            resolvedWaypointIndex(positions, scrolledPastPx = 106 + 94, tolerancePx = 24),
        )
        assertEquals(300, nextWaypointOffset(positions, scrolledPastPx = 106 + 94))
    }

    @Test
    fun waypointIndex_noSitesOrExhausted() {
        assertEquals(-1, resolvedWaypointIndex(emptyList(), scrolledPastPx = 500, tolerancePx = 24))
        // 走完最后一站后进度落在末位, 调用方据此回退到「跳楼」分支
        assertEquals(
            3,
            resolvedWaypointIndex(listOf(0, 100, 200, 300), scrolledPastPx = 900, tolerancePx = 24),
        )
        assertNull(nextWaypointOffset(listOf(0, 100, 200, 300), scrolledPastPx = 900))
    }

    @Test
    fun waypointWalk_eachPressMovesForward() {
        // 从楼顶逐站走完一整楼: 每次按键的目标都必须严格大于当前视口位置, 即不存在白按一下
        val positions = listOf(0, 100, 200, 300)
        var scrollPosition = 0
        val visited = mutableListOf<Int>()
        repeat(positions.size) {
            val next = nextWaypointOffset(positions, scrollPosition) ?: return@repeat
            assertTrue("每次按 ▼ 都应向下移动到下一个站点", next > scrollPosition)
            scrollPosition = next
            visited += next
        }

        assertEquals(listOf(100, 200, 300), visited)
    }

    @Test
    fun waypointWalk_recoversAfterManualScroll() {
        // 导航落到某楼(进度记忆为 0)后用户手动滚到楼层深处,
        // 再按下 ▼ 应当顺着实测视口继续往下, 而不是回到第一个站点
        val positions = listOf(0, 100, 200, 300)

        assertEquals(300, nextWaypointOffset(positions, scrolledPastPx = 250))
        // 对齐到最后一站之后再按下, 站点走完 -> 无下一站, 交给跳楼分支
        assertNull(nextWaypointOffset(positions, scrolledPastPx = 300))
    }
}
