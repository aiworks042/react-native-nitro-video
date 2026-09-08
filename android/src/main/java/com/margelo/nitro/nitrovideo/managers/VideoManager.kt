package com.margelo.nitro.nitrovideo.managers

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.NitroModules
import com.margelo.nitro.nitrovideo.FullscreenPlayerActivity
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.cache.VideoCache
import com.margelo.nitro.nitrovideo.listeners.VideoManagerListener
import com.margelo.nitro.nitrovideo.utils.weakMutableHashSetOf
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
object VideoManager {
    const val INTENT_PLAYER_KEY = "player_uuid"

    private val context: Context
        get() = NitroModules.applicationContext
            ?: throw IllegalStateException("NitroModules.applicationContext is null!")

    val cache: VideoCache by lazy {
        VideoCache(context)
    }

    val audioFocusManager: AudioFocusManager by lazy {
        AudioFocusManager(context)
    }

    val pictureInPicture: PictureInPictureManager by lazy {
        PictureInPictureManager()
    }

    private val videoViews = ConcurrentHashMap<String, HybridNitroVideoView>()
    private val fullscreenPlayerActivities = ConcurrentHashMap<String, WeakReference<FullscreenPlayerActivity>>()
    private val videoPlayersToVideoViews = ConcurrentHashMap<HybridVideoPlayer, MutableList<HybridNitroVideoView>>()
    private val playersRequestingKeepAwake = weakMutableHashSetOf<HybridVideoPlayer>()
    private val listeners = mutableListOf<WeakReference<VideoManagerListener>>()

    fun registerListener(listener: VideoManagerListener) {
        listeners.add(WeakReference(listener))
    }

    fun unregisterListener(listener: VideoManagerListener) {
        listeners.retainAll { it.get() != listener }
    }

    fun registerVideoView(videoView: HybridNitroVideoView) {
        videoViews[videoView.videoViewId] = videoView
        listeners.forEach {
            it.get()?.onVideoViewRegistered(videoView, videoViews.values)
        }
    }

    fun getVideoView(id: String): HybridNitroVideoView? {
        return videoViews[id]
    }

    fun unregisterVideoView(videoView: HybridNitroVideoView) {
        videoViews.remove(videoView.videoViewId)
        listeners.forEach {
            it.get()?.onVideoViewUnregistered(videoView, videoViews.values)
        }
    }

    fun registerVideoPlayer(videoPlayer: HybridVideoPlayer) {
        videoPlayersToVideoViews[videoPlayer] = videoPlayersToVideoViews[videoPlayer] ?: mutableListOf()
        audioFocusManager.registerPlayer(videoPlayer)
    }

    fun unregisterVideoPlayer(videoPlayer: HybridVideoPlayer) {
        videoPlayersToVideoViews.remove(videoPlayer)
        audioFocusManager.unregisterPlayer(videoPlayer)
    }

    fun registerFullscreenPlayerActivity(id: String, fullscreenActivity: FullscreenPlayerActivity) {
        fullscreenPlayerActivities[id] = WeakReference(fullscreenActivity)
    }

    fun unregisterFullscreenPlayerActivity(id: String) {
        fullscreenPlayerActivities.remove(id)
    }

    fun finishFullscreenPlayer(id: String) {
        fullscreenPlayerActivities[id]?.get()?.finish()
    }

    fun onVideoPlayerAttachedToView(videoPlayer: HybridVideoPlayer, videoView: HybridNitroVideoView) {
        val list = videoPlayersToVideoViews.getOrPut(videoPlayer) { mutableListOf() }
        if (!list.contains(videoView)) {
            list.add(videoView)
        }
        if (list.size == 1) {
            videoPlayer.serviceConnection?.playbackServiceBinder?.service?.registerPlayer(videoPlayer)
        }
    }

    fun onVideoPlayerDetachedFromView(videoPlayer: HybridVideoPlayer, videoView: HybridNitroVideoView) {
        videoPlayersToVideoViews[videoPlayer]?.remove(videoView)
        if (videoPlayersToVideoViews[videoPlayer].isNullOrEmpty()) {
            videoPlayer.exoPlayer?.let { exo ->
                videoPlayer.serviceConnection?.playbackServiceBinder?.service?.unregisterPlayer(exo)
            }
        }
    }

    fun requestKeepAwake(player: HybridVideoPlayer) {
        playersRequestingKeepAwake.add(player)
        applyKeepAwake()
    }

    fun releaseKeepAwake(player: HybridVideoPlayer) {
        playersRequestingKeepAwake.remove(player)
        applyKeepAwake()
    }

    fun isVideoPlayerAttachedToView(videoPlayer: HybridVideoPlayer): Boolean {
        return videoPlayersToVideoViews[videoPlayer]?.isNotEmpty() ?: false
    }

    fun hasRegisteredPlayers(): Boolean {
        return videoPlayersToVideoViews.isNotEmpty()
    }

    fun onAppForegrounded() {
        listeners.forEach {
            it.get()?.onAppForegrounded()
        }
    }

    fun onAppBackgrounded() {
        for (videoView in videoViews.values) {
            if (shouldPauseVideo(videoView)) {
                handleVideoPause(videoView)
            } else {
                videoView.wasAutoPaused = false
            }
        }
        listeners.forEach {
            it.get()?.onAppBackgrounded()
        }
    }

    private fun applyKeepAwake() {
        for (videoView in videoViews.values) {
            videoView.view.keepScreenOn = playersRequestingKeepAwake.isNotEmpty()
        }
    }

    private fun shouldPauseVideo(videoView: HybridNitroVideoView): Boolean {
        return videoView.connectedPlayer?.staysActiveInBackground == false &&
            !videoView.pipParams.autoEnter &&
            !videoView.pipParams.willEnter &&
            !videoView.isInFullscreen
    }

    private fun handleVideoPause(videoView: HybridNitroVideoView) {
        videoView.connectedPlayer?.exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                videoView.wasAutoPaused = true
            }
        }
    }
}
