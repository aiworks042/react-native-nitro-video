package com.margelo.nitro.nitrovideo.records

import com.margelo.nitro.nitrovideo.enums.VideoChangeFrameRateStrategy
import java.io.Serializable
import kotlin.time.Duration

class PlayerBuilderOptions(
    var seekBackwardIncrement: Duration? = null,
    var seekForwardIncrement: Duration? = null,
    var videoChangeFrameRateStrategy: VideoChangeFrameRateStrategy? = null
) : Serializable
