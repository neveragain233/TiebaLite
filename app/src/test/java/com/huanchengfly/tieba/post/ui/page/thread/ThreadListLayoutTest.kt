package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
