package com.huanchengfly.tieba.post.ui.utils

import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

fun getPhotoViewData(
    post: Post,
    content: PicContentRender,
    seeLz: Boolean = false
): PhotoViewData? {
    if (post.from_forum == null) return null
    return PhotoViewData(
        data = LoadPicPageData(
            forumId = post.from_forum.id,
            forumName = post.from_forum.name,
            threadId = post.tid,
            postId = post.id,
            objType = "pb",
            picId = content.picId,
            picIndex = 1,
            seeLz = seeLz,
            originUrl = content.originUrl,
        ),
        picItems = persistentListOf(
            PicItem(
                picId = content.picId,
                picIndex = 1,
                originUrl = content.originUrl,
                postId = post.id
            )
        )
    )
}

/**
 * 构建帖子全部图片的图集数据, 用于正文/楼层多图网格点击进入查看器.
 */
fun getPhotoViewData(
    post: Post,
    pics: List<PicContentRender>,
    index: Int,
    seeLz: Boolean = false
): PhotoViewData? {
    if (post.from_forum == null) return null
    return getPhotoViewData(
        forumId = post.from_forum.id,
        forumName = post.from_forum.name,
        threadId = post.tid,
        postId = post.id,
        pics = pics,
        index = index,
        seeLz = seeLz,
    )
}

/**
 * 构建图集数据, 供 SubPostsPage 等没有 Proto Post 上下文的场景使用.
 */
fun getPhotoViewData(
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long,
    pics: List<PicContentRender>,
    index: Int,
    seeLz: Boolean = false,
): PhotoViewData {
    val content = pics[index]
    return PhotoViewData(
        data = LoadPicPageData(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = postId,
            seeLz = seeLz,
            objType = "pb",
            picId = content.picId,
            picIndex = index + 1,
            originUrl = content.originUrl,
        ),
        picItems = pics.mapIndexed { picIndex, pic ->
            PicItem(
                picId = pic.picId,
                picIndex = picIndex + 1,
                originUrl = pic.originUrl,
                postId = postId,
            )
        }.toImmutableList(),
        index = index,
    )
}

fun getPhotoViewData(
    medias: List<Media>,
    forumId: Long,
    forumName: String,
    threadId: Long,
    index: Int
): PhotoViewData {
    val media = medias[index]
    return PhotoViewData(
        data = LoadPicPageData(
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = media.postId,
            seeLz = false,
            objType = "index",
            picId = ImageUtil.getPicId(media.originPic),
            picIndex = index + 1,
            originUrl = media.originPic
        ),
        picItems = medias.mapIndexed { mediaIndex, mediaItem ->
            PicItem(
                picId = ImageUtil.getPicId(mediaItem.originPic),
                picIndex = mediaIndex + 1,
                originUrl = mediaItem.originPic,
                postId = mediaItem.postId
            )
        }.toImmutableList(),
        index = index
    )
}
