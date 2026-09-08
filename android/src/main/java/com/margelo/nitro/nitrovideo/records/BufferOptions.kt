package com.margelo.nitro.nitrovideo.records

import java.io.Serializable

class BufferOptions(
    var preferredForwardBufferDuration: Double? = null,
    var maxBufferBytes: Long = 0,
    var prioritizeTimeOverSizeThreshold: Boolean = false,
    var minBufferForPlayback: Double = 1.0
) : Serializable
