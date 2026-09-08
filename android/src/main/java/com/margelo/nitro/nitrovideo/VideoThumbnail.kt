package com.margelo.nitro.nitrovideo

import android.graphics.Bitmap
import kotlin.time.Duration

class VideoThumbnail(
    val bitmap: Bitmap,
    val requestedTime: Duration,
    val actualTime: Duration
) {
    val width: Int = bitmap.width
    val height: Int = bitmap.height

    fun getAdditionalMemoryPressure(): Int {
        return bitmap.byteCount
    }
}
