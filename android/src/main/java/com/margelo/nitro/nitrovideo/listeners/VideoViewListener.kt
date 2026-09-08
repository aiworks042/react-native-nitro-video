package com.margelo.nitro.nitrovideo.listeners

import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.records.PiPParams

interface VideoViewListener {
    fun onPiPParamsChanged(videoView: HybridNitroVideoView, oldPiPParams: PiPParams, newPiPParams: PiPParams) {}
}
