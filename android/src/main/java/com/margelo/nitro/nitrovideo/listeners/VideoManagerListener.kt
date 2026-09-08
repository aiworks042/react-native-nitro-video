package com.margelo.nitro.nitrovideo.listeners

import com.margelo.nitro.nitrovideo.HybridNitroVideoView

interface VideoManagerListener {
    fun onVideoViewRegistered(videoView: HybridNitroVideoView, allVideoViews: Collection<HybridNitroVideoView>) {}
    fun onVideoViewUnregistered(videoView: HybridNitroVideoView, allVideoViews: Collection<HybridNitroVideoView>) {}
    fun onAppBackgrounded() {}
    fun onAppForegrounded() {}
}
