package com.margelo.nitro.nitrovideo.enums

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.C.COLOR_TRANSFER_HLG
import androidx.media3.common.C.COLOR_TRANSFER_ST2084
import androidx.media3.common.util.UnstableApi

enum class VideoRange(val value: String) {
    SDR("sdr"),
    HLG("hlg"),
    PQ("PQ");

    companion object {
        @OptIn(UnstableApi::class)
        fun fromCColorTransfer(colorTransfer: @C.ColorTransfer Int?): VideoRange {
            colorTransfer ?: return SDR
            return when (colorTransfer) {
                COLOR_TRANSFER_ST2084 -> PQ
                COLOR_TRANSFER_HLG -> HLG
                else -> SDR
            }
        }
    }
}
