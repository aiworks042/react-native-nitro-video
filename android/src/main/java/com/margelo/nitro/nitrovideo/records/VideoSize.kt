package com.margelo.nitro.nitrovideo.records

import androidx.media3.common.Format
import java.io.Serializable

data class VideoSize(
    val width: Int = 0,
    val height: Int = 0
) : Serializable {
    constructor(size: androidx.media3.common.VideoSize) : this(size.width, size.height)
    constructor(format: Format) : this(format.width, format.height)
}
