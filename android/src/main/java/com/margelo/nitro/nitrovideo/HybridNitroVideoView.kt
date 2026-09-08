package com.margelo.nitro.nitrovideo

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.CaptioningManager
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.uimanager.events.TouchEventCoalescingKeyHelper
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import com.margelo.nitro.nitrovideo.delegates.IgnoreSameSet
import com.margelo.nitro.nitrovideo.enums.ContentFit
import com.margelo.nitro.nitrovideo.listeners.VideoPlayerListener
import com.margelo.nitro.nitrovideo.listeners.VideoViewListener
import com.margelo.nitro.nitrovideo.managers.VideoManager
import com.margelo.nitro.nitrovideo.records.AudioTrack
import com.margelo.nitro.nitrovideo.records.ButtonOptions
import com.margelo.nitro.nitrovideo.records.FullscreenOptions
import com.margelo.nitro.nitrovideo.records.PiPParams
import com.margelo.nitro.nitrovideo.records.SubtitleTrack
import com.margelo.nitro.nitrovideo.records.VideoSource
import com.margelo.nitro.nitrovideo.records.VideoTrack
import com.margelo.nitro.nitrovideo.utils.calculatePiPAspectRatio
import com.margelo.nitro.nitrovideo.utils.SubtitleUtils
import com.margelo.nitro.nitrovideo.utils.calculateRectHint
import com.margelo.nitro.nitrovideo.utils.dispatchMotionEvent
import java.lang.ref.WeakReference
import java.util.UUID

@OptIn(UnstableApi::class)
class HybridNitroVideoView : HybridNitroVideoViewSpec(), VideoPlayerListener {

    val videoViewId: String = UUID.randomUUID().toString()

    private val context: Context
        get() = NitroModules.applicationContext
            ?: throw IllegalStateException("NitroModules.applicationContext is null!")

    private val currentActivity: Activity?
        get() = (NitroModules.applicationContext as? ReactContext)?.currentActivity

    private val mainHandler = Handler(Looper.getMainLooper())
    var connectedPlayer: HybridVideoPlayer? = null

    var wasAutoPaused: Boolean = false
    var isInFullscreen: Boolean = false
        private set
    var currentTrackHasSubtitles = false
        private set
    var showsAudioTracksButton = false
        private set

    var shouldHideSurfaceView: Boolean = true
    var buttonOptions: ButtonOptions = ButtonOptions()
        set(value) {
            field = value
            applyButtonSettings()
        }

    var fullscreenOptions: FullscreenOptions = FullscreenOptions()

    private val listeners = mutableListOf<WeakReference<VideoViewListener>>()
    private val touchEventCoalescingKeyHelper = TouchEventCoalescingKeyHelper()
    private var reactNativeEventDispatcher: EventDispatcher? = null
    private var captioningChangeListener: CaptioningManager.CaptioningChangeListener? = null

    var pipParams by IgnoreSameSet(PiPParams()) { new, old ->
        listeners.forEach {
            it.get()?.onPiPParamsChanged(this, old, new)
        }
    }
        private set

    // Touch forwarding custom PlayerView
    inner class NitroTouchPlayerView(context: Context) : PlayerView(context) {
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (!nativeControls) {
                event?.eventTime?.let {
                    touchEventCoalescingKeyHelper.addCoalescingKey(it)
                    reactNativeEventDispatcher?.dispatchMotionEvent(this, event, touchEventCoalescingKeyHelper)
                }
            }
            if (event?.actionMasked == MotionEvent.ACTION_UP) {
                performClick()
            }
            return true
        }

        override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
            if (nativeControls) {
                event?.eventTime?.let {
                    touchEventCoalescingKeyHelper.addCoalescingKey(it)
                    reactNativeEventDispatcher?.dispatchMotionEvent(this, MotionEvent.obtainNoHistory(event), touchEventCoalescingKeyHelper)
                }
            }
            return false
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            super.onLayout(changed, l, t, r, b)
            pipParams = pipParams.copy(rectHint = calculateRectHint(this))
            setTimeBarInteractive(requiresLinearPlayback)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            setupCaptioningChangeListener()
            SubtitleUtils.configureSubtitleView(this, context)
            pipParams = pipParams.copy(canEnter = true)
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            captioningChangeListener?.let { listener ->
                val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
                captioningManager?.removeCaptioningChangeListener(listener)
                captioningChangeListener = null
            }
            pipParams = pipParams.copy(canEnter = false)
        }
    }

    val playerView: PlayerView by lazy {
        NitroTouchPlayerView(context).apply {
            useController = false
            setShutterBackgroundColor(Color.TRANSPARENT)
            videoSurfaceView?.alpha = 0f
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            SubtitleUtils.configureSubtitleView(this, context)

            setFullscreenButtonClickListener {
                enterFullscreen()
            }
        }
    }

    val view: View
        get() = playerView

    init {
        VideoManager.registerVideoView(this)
        (NitroModules.applicationContext as? ReactContext)?.let { reactCtx ->
            reactNativeEventDispatcher = UIManagerHelper.getEventDispatcher(reactCtx, playerView.id)
        }
    }

    fun applySurfaceViewVisibility() {
        if (!useExoShutter && shouldHideSurfaceView) {
            playerView.videoSurfaceView?.alpha = 0f
        } else {
            playerView.videoSurfaceView?.alpha = 1f
        }
    }

    private fun setupCaptioningChangeListener() {
        val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        captioningChangeListener = SubtitleUtils.createCaptioningChangeListener(playerView, context)
        captioningChangeListener?.let { listener ->
            captioningManager?.addCaptioningChangeListener(listener)
        }
    }

    fun applyButtonSettings() {
        val shouldShowSubtitle = buttonOptions.showSubtitles ?: currentTrackHasSubtitles
        playerView.applyButtonOptions(buttonOptions, requiresLinearPlayback)
        playerView.setShowSubtitleButton(shouldShowSubtitle)
    }

    // ─── Player connection ───────────────────────────────────────────────────

    override var playerId: Double? = null
        set(value) {
            field = value
            mainHandler.post {
                val oldPlayer = connectedPlayer
                oldPlayer?.removeListener(this)
                oldPlayer?.let {
                    VideoManager.onVideoPlayerDetachedFromView(it, this)
                }

                val pid = value?.toInt()
                if (pid != null) {
                    val newPlayer = NitroVideoPlayerRegistry.get(pid)
                    connectedPlayer = newPlayer
                    playerView.player = newPlayer?.exoPlayer
                    newPlayer?.addListener(this)
                    newPlayer?.let {
                        VideoManager.onVideoPlayerAttachedToView(it, this)
                    }
                    val hasEmittedFirstFrame = newPlayer?.firstFrameEventGenerator?.hasSentFirstFrameForCurrentMediaItem ?: false
                    shouldHideSurfaceView = !hasEmittedFirstFrame
                    applySurfaceViewVisibility()
                } else {
                    connectedPlayer = null
                    playerView.player = null
                }
            }
        }

    override var nativeControls: Boolean = true
        set(value) {
            field = value
            mainHandler.post {
                playerView.useController = value
                applyButtonSettings()
            }
        }

    override var contentFit: VideoContentFit = VideoContentFit.CONTAIN
        set(value) {
            field = value
            mainHandler.post {
                playerView.resizeMode = when (value) {
                    VideoContentFit.COVER -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoContentFit.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    VideoContentFit.CONTAIN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        }

    override var allowsPictureInPicture: Boolean = false

    override var startsPictureInPictureAutomatically: Boolean = false
        set(value) {
            field = value
            pipParams = pipParams.copy(autoEnter = value)
        }

    override var requiresLinearPlayback: Boolean = false
        set(value) {
            field = value
            mainHandler.post {
                playerView.applyRequiresLinearPlayback(value)
                applyButtonSettings()
            }
        }

    override var useExoShutter: Boolean = false
        set(value) {
            field = value
            mainHandler.post {
                if (value) {
                    playerView.setShutterBackgroundColor(Color.BLACK)
                } else {
                    playerView.setShutterBackgroundColor(Color.TRANSPARENT)
                }
                applySurfaceViewVisibility()
            }
        }

    override var controllerAutoShow: Boolean = true
        set(value) {
            field = value
            mainHandler.post { playerView.controllerAutoShow = value }
        }

    // ─── Callbacks ───────────────────────────────────────────────────────────

    override var onPictureInPictureStart: (() -> Unit)? = null
    override var onPictureInPictureStop: (() -> Unit)? = null
    override var onFullscreenEnter: (() -> Unit)? = null
    override var onFullscreenExit: (() -> Unit)? = null
    override var onFirstFrameRender: (() -> Unit)? = null

    // ─── Imperative methods ──────────────────────────────────────────────────

    override fun enterFullscreen(): Promise<Unit> {
        mainHandler.post {
            val activity = currentActivity ?: return@post
            val pid = playerId?.toInt() ?: return@post
            isInFullscreen = true
            pipParams = pipParams.copy(blocksAppFromEntering = true)
            FullscreenPlayerActivity.start(activity, pid)
            onFullscreenEnter?.invoke()
        }
        return Promise.resolved(Unit)
    }

    override fun exitFullscreen(): Promise<Unit> {
        mainHandler.post {
            val fullScreenButton: ImageButton? = playerView.findViewById(androidx.media3.ui.R.id.exo_fullscreen)
            fullScreenButton?.setImageResource(androidx.media3.ui.R.drawable.exo_icon_fullscreen_enter)
            isInFullscreen = false
            pipParams = pipParams.copy(blocksAppFromEntering = false)
            onFullscreenExit?.invoke()
        }
        return Promise.resolved(Unit)
    }

    override fun startPictureInPicture(): Promise<Unit> {
        mainHandler.post {
            pipParams = pipParams.copy(willEnter = true)
            VideoManager.pictureInPicture.enterPictureInPicture(this)
        }
        return Promise.resolved(Unit)
    }

    override fun stopPictureInPicture(): Promise<Unit> {
        return Promise.resolved(Unit)
    }

    fun onStartPictureInPicture(pipCandidate: HybridNitroVideoView?) {
        onPictureInPictureStart?.invoke()
    }

    fun onStopPictureInPicture(pipCandidate: HybridNitroVideoView?) {
        pipParams = pipParams.copy(willEnter = false)
        onPictureInPictureStop?.invoke()
    }

    fun addVideoViewListener(listener: VideoViewListener) {
        if (listeners.none { it.get() == listener }) {
            listeners.add(WeakReference(listener))
        }
    }

    fun removeVideoViewListener(listener: VideoViewListener) {
        listeners.retainAll { it.get() != listener }
    }

    // ─── VideoPlayerListener callbacks ───────────────────────────────────────

    override fun onSourceChanged(player: HybridVideoPlayer, source: VideoSource?, oldSource: VideoSource?) {
        if (player == connectedPlayer && oldSource != null) {
            shouldHideSurfaceView = true
            applySurfaceViewVisibility()
        }
    }

    override fun onVideoSourceLoaded(
        player: HybridVideoPlayer,
        videoSource: VideoSource?,
        duration: Double?,
        availableVideoTracks: List<VideoTrack>,
        availableSubtitleTracks: List<SubtitleTrack>,
        availableAudioTracks: List<AudioTrack>
    ) {
        pipParams = pipParams.copy(
            aspectRatio = calculateCurrentPipAspectRatio()
        )
    }

    override fun onIsPlayingChanged(player: HybridVideoPlayer, isPlaying: Boolean, oldIsPlaying: Boolean?) {
        if (player == connectedPlayer && isPlaying) {
            wasAutoPaused = false
        }
    }

    override fun onTracksChanged(player: HybridVideoPlayer, tracks: Tracks) {
        currentTrackHasSubtitles = (player.tracksHelper?.availableSubtitleTracks?.isNotEmpty() == true)
        showsAudioTracksButton = ((player.tracksHelper?.availableAudioTracks?.size ?: 0) > 1)
        playerView.setShowSubtitleButton(buttonOptions.showSubtitles ?: currentTrackHasSubtitles)
    }

    override fun onRenderedFirstFrame(player: HybridVideoPlayer) {
        shouldHideSurfaceView = false
        applySurfaceViewVisibility()
        onFirstFrameRender?.invoke()
    }

    fun onDropView() {
        mainHandler.post {
            VideoManager.unregisterVideoView(this)
            connectedPlayer?.removeListener(this)
            connectedPlayer?.let {
                VideoManager.onVideoPlayerDetachedFromView(it, this)
            }
            connectedPlayer = null
            playerView.player = null
        }
    }
}

private fun HybridNitroVideoView.calculateCurrentPipAspectRatio(): android.util.Rational? {
    val player = connectedPlayer?.exoPlayer ?: return null
    val fit = when (contentFit) {
        VideoContentFit.COVER -> ContentFit.COVER
        VideoContentFit.FILL -> ContentFit.FILL
        VideoContentFit.CONTAIN -> ContentFit.CONTAIN
    }
    return calculatePiPAspectRatio(player.videoSize, view.width, view.height, fit)
}
