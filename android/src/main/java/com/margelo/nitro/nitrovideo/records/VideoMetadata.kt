package com.margelo.nitro.nitrovideo.records

import android.net.Uri
import java.io.Serializable

class VideoMetadata(
    var title: String? = null,
    var artist: String? = null,
    var artwork: Uri? = null
) : Serializable
