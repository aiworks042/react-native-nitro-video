package com.margelo.nitro.nitrovideo.enums

import androidx.media3.common.C
import java.util.UUID

enum class DRMType(val value: String) {
    CLEARKEY("clearkey"),
    FAIRPLAY("fairplay"),
    PLAYREADY("playready"),
    WIDEVINE("widevine");

    fun isSupported(): Boolean {
        return this != FAIRPLAY
    }

    fun toUUID(): UUID {
        return when (this) {
            CLEARKEY -> C.CLEARKEY_UUID
            FAIRPLAY -> throw UnsupportedOperationException("FairPlay DRM is only supported on Apple platforms (iOS/macOS).")
            PLAYREADY -> C.PLAYREADY_UUID
            WIDEVINE -> C.WIDEVINE_UUID
        }
    }

    companion object {
        fun fromString(str: String?): DRMType? {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) }
        }
    }
}
