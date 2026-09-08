package com.margelo.nitro.nitrovideo.records

import java.io.Serializable

enum class FullscreenOrientation(val value: String) {
    DEFAULT("default"),
    PORTRAIT("portrait"),
    PORTRAIT_UP("portraitUp"),
    PORTRAIT_DOWN("portraitDown"),
    LANDSCAPE("landscape"),
    LANDSCAPE_LEFT("landscapeLeft"),
    LANDSCAPE_RIGHT("landscapeRight");
}

data class FullscreenOptions(
    var autoExitOnRotate: Boolean = false,
    var orientation: FullscreenOrientation = FullscreenOrientation.DEFAULT
) : Serializable
