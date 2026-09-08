package com.margelo.nitro.nitrovideo.records

import androidx.media3.common.MediaItem
import com.margelo.nitro.nitrovideo.enums.DRMType
import java.io.Serializable

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
