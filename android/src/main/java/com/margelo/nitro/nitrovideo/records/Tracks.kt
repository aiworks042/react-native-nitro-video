package com.margelo.nitro.nitrovideo.records

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.enums.VideoRange
import java.io.Serializable
import java.util.Locale

class SubtitleTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val name: String?,
    val isDefault: Boolean,
    val autoSelect: Boolean
) : Serializable {
    companion object {
        fun fromFormat(format: Format?): SubtitleTrack? {
            format ?: return null
            val id = format.id ?: return null
            val language = format.language ?: return null
            val label = Locale(language).displayLanguage
            val name = format.label
            val isDefault = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0
            val autoSelect = (format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT) != 0

            return SubtitleTrack(
                id = id,
                language = language,
                label = label,
                name = name,
                isDefault = isDefault,
                autoSelect = autoSelect
            )
        }
    }
}

class AudioTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val name: String?,
    val isDefault: Boolean,
    val autoSelect: Boolean
) : Serializable {
    companion object {
        fun fromFormat(format: Format?): AudioTrack? {
            format ?: return null
            val id = format.id ?: return null
            val language = format.language
            val label = language?.let { Locale(it).displayLanguage } ?: "Unknown"
            val name = format.label
            val isDefault = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0
            val autoSelect = (format.selectionFlags and C.SELECTION_FLAG_AUTOSELECT) != 0

            return AudioTrack(
                id = id,
                language = language,
                label = label,
                name = name,
                isDefault = isDefault,
                autoSelect = autoSelect
            )
        }
    }
}

@OptIn(UnstableApi::class)
class VideoTrack(
    val id: String,
    val url: Uri?,
    val size: VideoSize,
    val mimeType: String?,
    val isSupported: Boolean = true,
    val bitrate: Int? = null,
    val averageBitrate: Int? = null,
    val peakBitrate: Int? = null,
    val frameRate: Float? = null,
    val videoRange: VideoRange = VideoRange.SDR,
    var format: Format? = null
) : Serializable {
    companion object {
        fun fromFormat(format: Format?, isSupported: Boolean, variantUrl: Uri?): VideoTrack? {
            val id = format?.id ?: return null
            val size = VideoSize(format)
            val mimeType = format.sampleMimeType
            val averageBitrate = format.averageBitrate.takeIf { it != Format.NO_VALUE }
            val peakBitrate = format.peakBitrate.takeIf { it != Format.NO_VALUE }
            val frameRate = format.frameRate.takeIf { it != Format.NO_VALUE.toFloat() }
            val videoRange = VideoRange.fromCColorTransfer(format.colorInfo?.colorTransfer)

            return VideoTrack(
                id = id,
                url = variantUrl,
                size = size,
                mimeType = mimeType,
                isSupported = isSupported,
                bitrate = averageBitrate ?: peakBitrate,
                averageBitrate = averageBitrate,
                peakBitrate = peakBitrate,
                frameRate = frameRate,
                videoRange = videoRange,
                format = format
            )
        }
    }
}
