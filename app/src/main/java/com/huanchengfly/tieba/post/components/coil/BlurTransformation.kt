package com.huanchengfly.tieba.post.components.coil

import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor

class BlurTransformation(
    private val imgProcessor: ImageProcessor,
    val radius: Float
) : Transformation() {

    override val cacheKey = "${this::class.qualifiedName}-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return synchronized(imgProcessor) {
            imgProcessor.configureInputAndOutput(input)
            imgProcessor.blur(radius)
        }
    }

    override fun hashCode(): Int = 31 * cacheKey.hashCode() + radius.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return radius == (other as BlurTransformation).radius
    }
}