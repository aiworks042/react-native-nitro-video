package com.margelo.nitro.nitrovideo.records

import java.io.Serializable

class VideoThumbnailOptions(
    val maxWidth: Int? = null,
    val maxHeight: Int? = null
) : Serializable {
    fun toNativeSizeLimit(): Pair<Int, Int>? {
        if (this.maxWidth == null && this.maxHeight == null) {
            return null
        }
        val width = this.maxWidth ?: Int.MAX_VALUE
        val height = this.maxHeight ?: Int.MAX_VALUE

        require(width >= 1 && height >= 1) {
            "Failed to generate a thumbnail: The maxWidth and maxHeight parameters must be greater than zero"
        }
        return width to height
    }
}
