package com.margelo.nitro.nitrovideo.enums

import androidx.media3.common.MimeTypes

enum class ContentType(val value: String) {
    AUTO("auto"),
    PROGRESSIVE("progressive"),
    HLS("hls"),
    DASH("dash"),
    SMOOTH_STREAMING("smoothStreaming");

    fun toMimeTypeString(): String? {
        return when (this) {
            AUTO -> null
            PROGRESSIVE -> null
            HLS -> MimeTypes.APPLICATION_M3U8
            DASH -> MimeTypes.APPLICATION_MPD
            SMOOTH_STREAMING -> MimeTypes.APPLICATION_SS
        }
    }

    companion object {
        fun fromString(str: String?): ContentType {
            return when (str?.lowercase()) {
                "progressive" -> PROGRESSIVE
                "hls" -> HLS
                "dash" -> DASH
                "smoothstreaming" -> SMOOTH_STREAMING
                else -> AUTO
            }
        }
    }
}
