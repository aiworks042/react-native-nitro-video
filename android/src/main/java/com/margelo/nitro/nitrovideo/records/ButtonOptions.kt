package com.margelo.nitro.nitrovideo.records

import java.io.Serializable

data class ButtonOptions(
    val showNext: Boolean = false,
    val showPrevious: Boolean = false,
    val showSeekForward: Boolean = true,
    val showSeekBackward: Boolean = true,
    val showSubtitles: Boolean? = null,
    val showSettings: Boolean = true,
    val showPlayPause: Boolean = true,
    val showBottomBar: Boolean = true
) : Serializable
