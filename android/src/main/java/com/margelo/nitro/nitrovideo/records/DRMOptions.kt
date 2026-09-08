package com.margelo.nitro.nitrovideo.records

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import java.io.Serializable
import java.util.UUID

enum class DRMType(val value: String) {
    CLEARKEY("clearkey"),
    PLAYREADY("playready"),
    WIDEVINE("widevine");

    fun toUUID(): UUID {
        return when (this) {
            CLEARKEY -> C.CLEARKEY_UUID
            PLAYREADY -> C.PLAYREADY_UUID
            WIDEVINE -> C.WIDEVINE_UUID
        }
    }
}

data class DRMOptions(
    var type: DRMType = DRMType.WIDEVINE,
    var licenseServer: String? = null,
    var headers: Map<String, String>? = null,
    var multiKey: Boolean = false
) : Serializable {

    fun toDRMConfiguration(): MediaItem.DrmConfiguration = MediaItem
        .DrmConfiguration
        .Builder(type.toUUID())
        .apply {
            licenseServer?.let { setLicenseUri(it) }
            headers?.let { setLicenseRequestHeaders(it) }
            setMultiSession(multiKey)
        }
        .build()
}
