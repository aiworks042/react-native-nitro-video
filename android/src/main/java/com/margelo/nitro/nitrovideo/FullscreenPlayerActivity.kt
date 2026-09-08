package com.margelo.nitro.nitrovideo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.accessibility.CaptioningManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.margelo.nitro.nitrovideo.enums.ContentFit
import com.margelo.nitro.nitrovideo.listeners.VideoManagerListener
import com.margelo.nitro.nitrovideo.managers.VideoManager
import com.margelo.nitro.nitrovideo.records.FullscreenOptions
import com.margelo.nitro.nitrovideo.utils.FullscreenActivityOrientationHelper
import com.margelo.nitro.nitrovideo.utils.SubtitleUtils
import com.margelo.nitro.nitrovideo.utils.applyPiPParams
import com.margelo.nitro.nitrovideo.utils.applyRectHint
import com.margelo.nitro.nitrovideo.utils.calculatePiPAspectRatio
import com.margelo.nitro.nitrovideo.utils.calculateRectHint

@OptIn(UnstableApi::class)
class FullscreenPlayerActivity : Activity(), VideoManagerListener {

    private lateinit var mContentView: FrameLayout
    private var videoViewId: String? = null
    private var videoPlayer: HybridVideoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var videoView: HybridNitroVideoView
    private var didFinish = false
    private var wasAutoPaused = false
    private var isStopped = false
    private var options = FullscreenOptions()
    private var orientationHelper: FullscreenActivityOrientationHelper? = null
    private var captioningChangeListener: CaptioningManager.CaptioningChangeListener? = null

    companion object {
        const val INTENT_FULLSCREEN_OPTIONS_KEY = "fullscreen_options"

        fun start(context: Context, playerId: Int) {
            val player = NitroVideoPlayerRegistry.get(playerId) ?: return
            val connectedView = player.currentVideoViewRef.get()
            val intent = Intent(context, FullscreenPlayerActivity::class.java).apply {
                putExtra(VideoManager.INTENT_PLAYER_KEY, connectedView?.videoViewId ?: "")
                putExtra(INTENT_FULLSCREEN_OPTIONS_KEY, connectedView?.fullscreenOptions ?: FullscreenOptions())
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            videoViewId = intent.getStringExtra(VideoManager.INTENT_PLAYER_KEY)
            options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(INTENT_FULLSCREEN_OPTIONS_KEY, FullscreenOptions::class.java)
                    ?: FullscreenOptions()
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(INTENT_FULLSCREEN_OPTIONS_KEY) as? FullscreenOptions
                    ?: FullscreenOptions()
            }

            val targetView = videoViewId?.let { VideoManager.getVideoView(it) }
            if (targetView == null) {
                finish()
                return
            }
            videoView = targetView

            orientationHelper = FullscreenActivityOrientationHelper(
                this,
                options,
                onShouldAutoExit = { finish() },
                onShouldReleaseOrientation = {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            )
            orientationHelper?.startOrientationEventListener()
        } catch (e: Exception) {
            Log.e("NitroVideo", "Failed to start fullscreen activity", e)
            finish()
            return
        }

        mContentView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mContentView.addView(playerView)
        setContentView(mContentView)

        requestedOrientation = options.orientation.toActivityOrientation()

        videoPlayer = videoView.connectedPlayer
        videoPlayer?.exoPlayer?.let { exo ->
            PlayerView.switchTargetView(exo, videoView.playerView, playerView)
        }

        videoViewId?.let { VideoManager.registerFullscreenPlayerActivity(it, this) }
        VideoManager.registerListener(this)

        playerView.player?.let { exo ->
            val fit = ContentFit.fromString(videoView.contentFit.name)
            val aspectRatio = calculatePiPAspectRatio(exo.videoSize, playerView.width, playerView.height, fit)
            applyPiPParams(this, videoView.startsPictureInPictureAutomatically, aspectRatio)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        hideStatusBar()
        setupFullscreenButton()

        val requiresLinearPlayback = videoView.requiresLinearPlayback
        val buttonConfig = videoView.buttonOptions.copy(showBottomBar = true)
        playerView.applyButtonOptions(buttonConfig, requiresLinearPlayback)
        playerView.setTimeBarInteractive(requiresLinearPlayback)
        playerView.controllerAutoShow = videoView.controllerAutoShow

        playerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            playerView.setTimeBarInteractive(requiresLinearPlayback)
            applyRectHint(this, calculateRectHint(playerView))
        }

        playerView.setShowSubtitleButton(videoView.buttonOptions.showSubtitles ?: videoView.currentTrackHasSubtitles)
        SubtitleUtils.configureSubtitleView(playerView, this)
        setupCaptioningChangeListener()
    }

    override fun finish() {
        super.finish()
        didFinish = true
        videoViewId?.let { id ->
            val v = VideoManager.getVideoView(id)
            videoPlayer?.exoPlayer?.let { exo ->
                v?.let { PlayerView.switchTargetView(exo, playerView, it.playerView) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        orientationHelper?.startOrientationEventListener()
        playerView.useController = true
        SubtitleUtils.configureSubtitleView(playerView, this)
        super.onResume()
    }

    override fun onPause() {
        if (videoPlayer?.staysActiveInBackground != true && !didFinish) {
            wasAutoPaused = videoPlayer?.exoPlayer?.isPlaying == true
            if (wasAutoPaused) {
                playerView.useController = false
                videoPlayer?.exoPlayer?.pause()
            }
        }
        orientationHelper?.stopOrientationEventListener()
        super.onPause()
    }

    override fun onStop() {
        isStopped = true
        super.onStop()
    }

    override fun onStart() {
        isStopped = false
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        captioningChangeListener?.let {
            val captioningManager = getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
            captioningManager?.removeCaptioningChangeListener(it)
            captioningChangeListener = null
        }

        if (::videoView.isInitialized) {
            videoView.exitFullscreen()
        }
        VideoManager.unregisterListener(this)
        videoViewId?.let { VideoManager.unregisterFullscreenPlayerActivity(it) }
        orientationHelper?.stopOrientationEventListener()
    }

    override fun onAppForegrounded() {
        if (isStopped || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) {
            finish()
        }
    }

    private fun setupFullscreenButton() {
        playerView.setFullscreenButtonClickListener { finish() }
        val fullScreenButton: ImageButton? = playerView.findViewById(androidx.media3.ui.R.id.exo_fullscreen)
        fullScreenButton?.setImageResource(androidx.media3.ui.R.drawable.exo_icon_fullscreen_exit)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if (!isInPictureInPictureMode) {
            playerView.useController = videoView.nativeControls
        } else {
            playerView.useController = false
        }
        if (wasAutoPaused && isInPictureInPictureMode) {
            videoPlayer?.exoPlayer?.play()
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }
    }

    private fun setupCaptioningChangeListener() {
        val captioningManager = getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        captioningChangeListener = SubtitleUtils.createCaptioningChangeListener(playerView, this)
        captioningChangeListener?.let { listener ->
            captioningManager?.addCaptioningChangeListener(listener)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationHelper?.onConfigurationChanged(newConfig)
    }
}
