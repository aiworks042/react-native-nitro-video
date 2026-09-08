package com.margelo.nitro.nitrovideo.records

import com.margelo.nitro.nitrovideo.enums.FullscreenOrientation
import java.io.Serializable

data class FullscreenOptions(
    var autoExitOnRotate: Boolean = false,
    var orientation: FullscreenOrientation = FullscreenOrientation.DEFAULT
) : Serializable
