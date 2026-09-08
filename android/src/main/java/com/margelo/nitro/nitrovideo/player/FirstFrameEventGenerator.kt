package com.margelo.nitro.nitrovideo.player

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.enums.ContentFit
import com.margelo.nitro.nitrovideo.listeners.VideoPlayerListener
import com.margelo.nitro.nitrovideo.utils.MutableWeakReference
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * Workaround around the `onRenderedFirstFrame` and `SurfaceView` layout race condition bug.
 * Ensures that the `onFirstFrame` event is sent after the `SurfaceView` is fully laid out.
 * https://github.com/google/ExoPlayer/issues/5222
 */
@OptIn(UnstableApi::class)
@MainThread
internal class FirstFrameEventGenerator(
    videoPlayer: HybridVideoPlayer,
    private val currentViewReference: MutableWeakReference<HybridNitroVideoView?>,
    private var onFirstFrameRendered: (() -> Unit)?
) : Player.Listener, VideoPlayerListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val videoPlayerReference = WeakReference(videoPlayer)
    private var hasPendingOnFirstFrame = false
    internal var hasSentFirstFrameForCurrentMediaItem = false
        private set
    internal var hasSentFirstFrameForCurrentVideoView = false
        private set

    init {
        videoPlayer.addListener(this)
        mainHandler.post {
            videoPlayer.exoPlayer.addListener(this@FirstFrameEventGenerator)
        }
    }

    @MainThread
    fun release() {
        videoPlayerReference.get()?.removeListener(this)
        videoPlayerReference.get()?.exoPlayer?.removeListener(this)
        onFirstFrameRendered = null
    }

    override fun onRenderedFirstFrame() {
        if (isPlayerSurfaceLayoutValid()) {
            maybeCallOnFirstFrameRendered()
        } else {
            hasPendingOnFirstFrame = true
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        hasSentFirstFrameForCurrentMediaItem = false
        super.onMediaItemTransition(mediaItem, reason)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        if (isPlayerSurfaceLayoutValid() && hasPendingOnFirstFrame) {
            maybeCallOnFirstFrameRendered()
        }
    }

    override fun onTargetViewChanged(player: HybridVideoPlayer, newTargetView: HybridNitroVideoView?, oldTargetView: HybridNitroVideoView?) {
        hasSentFirstFrameForCurrentVideoView = false
    }

    private fun maybeCallOnFirstFrameRendered() {
        if (!hasSentFirstFrameForCurrentMediaItem || !hasSentFirstFrameForCurrentVideoView) {
            onFirstFrameRendered?.invoke()
        }
        hasPendingOnFirstFrame = false
        hasSentFirstFrameForCurrentMediaItem = true
        hasSentFirstFrameForCurrentVideoView = true
    }

    private fun isPlayerSurfaceLayoutValid(): Boolean {
        val epsilon = 0.05
        val player = videoPlayerReference.get()?.exoPlayer ?: return false
        val currentPlayerView = currentViewReference.get() ?: return false
        val surfaceWidth = player.surfaceSize.width
        val surfaceHeight = player.surfaceSize.height
        val sourceWidth = player.videoSize.width
        val sourceHeight = player.videoSize.height
        val sourcePixelWidthHeightRatio = player.videoSize.pixelWidthHeightRatio

        if (surfaceWidth == 0 || surfaceHeight == 0) {
            return false
        }

        val surfaceAspectRatio = surfaceWidth.toFloat() / surfaceHeight
        val trackAspectRatio = sourceWidth.toFloat() / sourceHeight * sourcePixelWidthHeightRatio

        val videoSizeIsUnknown = sourceWidth == 0 || sourceHeight == 0
        val hasFillContentFit = currentPlayerView.playerView.resizeMode == ContentFit.FILL.toResizeMode()
        val hasCorrectRatio = abs(trackAspectRatio - surfaceAspectRatio) < epsilon

        return (hasCorrectRatio || hasFillContentFit || videoSizeIsUnknown)
    }
}
