package com.margelo.nitro.nitrovideo

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import com.margelo.nitro.nitrovideo.records.VideoThumbnailOptions
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class VideoThumbnailResult(
    val bitmap: Bitmap,
    val requestedTime: Duration,
    val actualTime: Duration,
    val width: Int = bitmap.width,
    val height: Int = bitmap.height
)

suspend fun <T> MediaMetadataRetriever.safeUse(block: suspend MediaMetadataRetriever.() -> T): T {
    try {
        return block()
    } finally {
        try {
            this.close()
        } catch (_: Exception) {}
    }
}

fun MediaMetadataRetriever.generateThumbnailAtTime(
    requestedTime: Duration,
    options: VideoThumbnailOptions? = null
): VideoThumbnailResult {
    val sizeLimit = options?.toNativeSizeLimit()

    val bitmap = if (sizeLimit != null) {
        val (maxWidth, maxHeight) = sizeLimit
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getScaledFrameAtTime(
                requestedTime.inWholeMicroseconds,
                MediaMetadataRetriever.OPTION_CLOSEST,
                maxWidth,
                maxHeight
            )
        } else {
            getFrameAtTime(requestedTime.inWholeMicroseconds, MediaMetadataRetriever.OPTION_CLOSEST)
                ?.constrainToDimensions(maxWidth, maxHeight)
        }
    } else {
        getFrameAtTime(requestedTime.inWholeMicroseconds, MediaMetadataRetriever.OPTION_CLOSEST)
    } ?: throw IllegalStateException("Failed to generate thumbnail")

    val actualTime = calculateActualFrameTime(this, requestedTime)
    return VideoThumbnailResult(bitmap, requestedTime, actualTime)
}

private fun calculateActualFrameTime(mediaMetadataRetriever: MediaMetadataRetriever, time: Duration): Duration {
    val frameTime = mediaMetadataRetriever.frameTime() ?: return time
    val frameIndex = (time.inWholeMicroseconds.toDouble() / frameTime).roundToLong()
    return (frameIndex * frameTime).toDuration(DurationUnit.MICROSECONDS)
}

private fun MediaMetadataRetriever.frameTime(): Double? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return null
    }

    val duration = this.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDouble() ?: return null
    val frameCount = this.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toDouble() ?: return null
    if (frameCount == 0.0) return null

    return (duration * 1000) / frameCount
}

private fun Bitmap.constrainToDimensions(maxWidth: Int, maxHeight: Int): Bitmap {
    val ratio = max(this.width / maxWidth.toFloat(), this.height / maxHeight.toFloat())
    if (ratio <= 1) return this

    val newWidth = (this.width / ratio).toInt()
    val newHeight = (this.height / ratio).toInt()
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}
