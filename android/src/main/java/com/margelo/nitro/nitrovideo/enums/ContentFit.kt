package com.margelo.nitro.nitrovideo.enums

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

@OptIn(UnstableApi::class)
enum class ContentFit(val value: String) {
    CONTAIN("contain"),
    FILL("fill"),
    COVER("cover");

    fun toResizeMode(): Int {
        return when (this) {
            CONTAIN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            COVER -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    }

    companion object {
        fun fromString(str: String): ContentFit {
            return when (str) {
                "cover" -> COVER
                "fill" -> FILL
                else -> CONTAIN
            }
        }
    }
}
