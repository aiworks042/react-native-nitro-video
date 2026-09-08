package com.margelo.nitro.nitrovideo.enums

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
enum class VideoChangeFrameRateStrategy(val value: String) {
    OFF("off"),
    ONLY_IF_SEAMLESS("onlyIfSeamless");

    fun toMedia3Strategy(): Int = when (this) {
        OFF -> C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF
        ONLY_IF_SEAMLESS -> C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
    }

    companion object {
        fun fromString(str: String?): VideoChangeFrameRateStrategy {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) } ?: ONLY_IF_SEAMLESS
        }
    }
}
