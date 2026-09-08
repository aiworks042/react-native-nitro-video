package com.margelo.nitro.nitrovideo.managers

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.facebook.react.bridge.ReactContext
import com.margelo.nitro.NitroModules
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.enums.ContentFit
import com.margelo.nitro.nitrovideo.listeners.VideoManagerListener
import com.margelo.nitro.nitrovideo.listeners.VideoViewListener
import com.margelo.nitro.nitrovideo.records.PiPParams
import com.margelo.nitro.nitrovideo.utils.PictureInPictureFragmentListener
import com.margelo.nitro.nitrovideo.utils.PictureInPictureHelperFragment
import com.margelo.nitro.nitrovideo.utils.applyPiPParams
import com.margelo.nitro.nitrovideo.utils.applyRectHint
import com.margelo.nitro.nitrovideo.utils.calculatePiPAspectRatio
import com.margelo.nitro.nitrovideo.utils.calculateRectHint
import com.margelo.nitro.nitrovideo.utils.isVisibleOnScreen
import com.margelo.nitro.nitrovideo.utils.visiblePercentage
import java.lang.ref.WeakReference

@OptIn(UnstableApi::class)
class PictureInPictureManager : PictureInPictureFragmentListener, VideoManagerListener, VideoViewListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val currentActivity: Activity?
        get() = (NitroModules.applicationContext as? ReactContext)?.currentActivity

    var autoEnterPiP = false
        private set

    var isInPiP = false
        private set

    var currentPiPViewCandidate = WeakReference<HybridNitroVideoView>(null)
        private set

    private var pipHelperFragment: PictureInPictureHelperFragment? = null
    private val videoViews = mutableListOf<WeakReference<HybridNitroVideoView>>()
    private val strongVideoViews
        get() = videoViews.mapNotNull { it.get() }
    private val rootViewChildrenOriginalVisibility = mutableMapOf<Int, Int>()
    private var isReleased = false

    init {
        VideoManager.registerListener(this)
    }

    fun release() {
        if (isInPiP) {
            layoutForPiPExit(null)
        }
        videoViews.clear()
        removePiPHelperFragment()
        isReleased = true
    }

    private fun findAndSetupPipCandidate() {
        val newCandidate = findAutoPiPViewCandidate(strongVideoViews)
        currentPiPViewCandidate = WeakReference(newCandidate)

        val noBlockingView = strongVideoViews.none { it.pipParams.blocksAppFromEntering }
        val newAutoEnter = noBlockingView && strongVideoViews.any { it.pipParams.autoEnter && it.pipParams.canEnter }
        autoEnterPiP = newAutoEnter

        if (!newAutoEnter) {
            currentActivity?.let {
                applyPiPParams(it, autoEnterPiP)
            }
        }

        newCandidate?.let {
            applyPipParamsForView(it)
        }
    }

    fun enterPictureInPicture(videoView: HybridNitroVideoView) {
        val activity = currentActivity ?: return
        currentPiPViewCandidate = WeakReference(videoView)
        applyPipParamsForView(videoView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            activity.enterPictureInPictureMode()
        }
    }

    private fun applyPipParamsForView(view: HybridNitroVideoView) {
        currentActivity?.let {
            applyRectHint(it, calculateRectHint(view.playerView))
            applyPiPParams(it, autoEnterPiP, view.pipParams.aspectRatio)
        }
    }

    override fun onPiPParamsChanged(videoView: HybridNitroVideoView, oldPiPParams: PiPParams, newPiPParams: PiPParams) {
        val pipParamsInfluenceRectHint = pipParamChangeInfluenceRectHint(oldPiPParams, newPiPParams)
        if (paramChangeInfluencesAutoEnter(oldPiPParams, newPiPParams)) {
            findAndSetupPipCandidate()
        } else if (videoView == currentPiPViewCandidate.get() && pipParamsInfluenceRectHint) {
            applyPipParamsForView(videoView)
        }
    }

    private fun pipParamChangeInfluenceRectHint(old: PiPParams, new: PiPParams): Boolean {
        return old.rectHint != new.rectHint || old.aspectRatio != new.aspectRatio
    }

    override fun onPictureInPictureStart() {
        maybeWarnAboutAutoEnterViews()
        val candidate = currentPiPViewCandidate.get()
        candidate?.let {
            layoutForPiPEnter(it)
        }

        strongVideoViews.forEach {
            it.onStartPictureInPicture(candidate)
        }

        isInPiP = true
    }

    override fun onPictureInPictureStop() {
        if (!isInPiP) return

        currentPiPViewCandidate.get()?.let {
            layoutForPiPExit(it)
        }
        strongVideoViews.forEach {
            it.onStopPictureInPicture(currentPiPViewCandidate.get())
        }

        isInPiP = false
    }

    override fun onVideoViewRegistered(videoView: HybridNitroVideoView, allVideoViews: Collection<HybridNitroVideoView>) {
        videoViews.add(WeakReference(videoView))
        if (videoViews.size == 1) {
            addPiPHelperFragment()
        }
        videoView.addVideoViewListener(this)
        electAutoPipViewCandidate()
    }

    override fun onVideoViewUnregistered(videoView: HybridNitroVideoView, allVideoViews: Collection<HybridNitroVideoView>) {
        videoViews.retainAll { it.get() != videoView }
        if (videoViews.isEmpty()) {
            removePiPHelperFragment()
        }
        videoView.removeVideoViewListener(this)
        findAndSetupPipCandidate()
    }

    override fun onAppBackgrounded() {
        val enteringVideoView = strongVideoViews.firstOrNull { it.pipParams.willEnter }
        val candidate = enteringVideoView ?: findAutoPiPViewCandidate(strongVideoViews)
        currentPiPViewCandidate = WeakReference(candidate)

        candidate?.let {
            applyPipParamsForView(it)
        }

        mainHandler.post {
            strongVideoViews.forEach {
                if (shouldPauseOnBackground(it)) {
                    it.connectedPlayer?.exoPlayer?.pause()
                    it.wasAutoPaused = true
                }
                it.playerView.useController = false
            }
        }
    }

    override fun onAppForegrounded() {
        mainHandler.post {
            strongVideoViews.forEach {
                it.playerView.useController = it.nativeControls
            }
        }
    }

    private fun addPiPHelperFragment() {
        (currentActivity as? FragmentActivity)?.let {
            val fragment = PictureInPictureHelperFragment(this)
            pipHelperFragment = fragment
            it.supportFragmentManager.beginTransaction()
                .add(fragment, fragment.id)
                .commitAllowingStateLoss()
        }
    }

    private fun removePiPHelperFragment() {
        pipHelperFragment?.release()
        (currentActivity as? FragmentActivity)?.let {
            val fragment = pipHelperFragment ?: return
            it.supportFragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }

    private fun electAutoPipViewCandidate() {
        if (isInPiP) return
        val newCandidate = findAutoPiPViewCandidate(strongVideoViews)
        currentPiPViewCandidate = WeakReference(newCandidate)
    }

    private fun findAutoPiPViewCandidate(videoViews: List<HybridNitroVideoView>): HybridNitroVideoView? {
        val pipViews = videoViews.filter { it.pipParams.autoEnter }
        val visiblePiPView = pipViews.filter { it.view.isVisibleOnScreen() }.sortedBy { it.view.visiblePercentage() }.reversed()
        val playingPipViews = pipViews.filter { it.connectedPlayer?.playing == true || it.wasAutoPaused }
        val visiblePlayingPipViews = visiblePiPView.filter { it.connectedPlayer?.playing == true }
        val relevanceOrderedViews = listOf(visiblePlayingPipViews, playingPipViews, visiblePiPView, pipViews).flatten()

        return relevanceOrderedViews.firstOrNull()
    }

    private fun maybeWarnAboutAutoEnterViews() {
        val pipViews = strongVideoViews.filter { it.startsPictureInPictureAutomatically }
        if (pipViews.size > 1) {
            Log.w(
                "NitroVideo",
                "Detected multiple VideoViews with startsPictureInPictureAutomatically set to true."
            )
        }
    }

    private fun layoutForPiPEnter(videoView: HybridNitroVideoView) {
        val activity = currentActivity ?: return
        val playerView = videoView.playerView
        val decorView = activity.window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        (playerView.parent as? ViewGroup)?.removeView(playerView)
        for (i in 0 until rootView.childCount) {
            val child = rootView.getChildAt(i)
            if (child != playerView) {
                rootViewChildrenOriginalVisibility[child.id] = child.visibility
                rootView.getChildAt(i).visibility = View.GONE
            }
        }
        rootView.addView(playerView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun layoutForPiPExit(videoView: HybridNitroVideoView?) {
        val activity = currentActivity ?: return
        val playerView = videoView?.playerView ?: return
        val decorView = activity.window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        rootView.removeView(playerView)
        (videoView.view as? ViewGroup)?.addView(playerView)

        for (i in 0 until rootView.childCount) {
            val child = rootView.getChildAt(i)
            rootViewChildrenOriginalVisibility[child.id]?.let {
                child.visibility = it
            }
        }
        rootViewChildrenOriginalVisibility.clear()
    }

    private fun paramChangeInfluencesAutoEnter(previous: PiPParams, new: PiPParams): Boolean {
        return previous.autoEnter != new.autoEnter || previous.canEnter != new.canEnter || previous.blocksAppFromEntering != new.blocksAppFromEntering
    }

    private fun shouldPauseOnBackground(videoView: HybridNitroVideoView): Boolean {
        if (videoView.isInFullscreen) return false
        if (strongVideoViews.any { it.isInFullscreen }) return true

        val candidate = currentPiPViewCandidate.get()
        val params = videoView.pipParams
        val isHandledByPiPManager = params.autoEnter || params.willEnter
        val isCandidate = videoView == candidate

        if (candidate == null) {
            return !(videoView.connectedPlayer?.staysActiveInBackground ?: false)
        }
        return isHandledByPiPManager && !isCandidate
    }
}

fun HybridNitroVideoView.calculateCurrentPipAspectRatio(): Rational? {
    val player = connectedPlayer?.exoPlayer ?: return null
    val contentFitEnum = ContentFit.fromString(contentFit.name)
    return calculatePiPAspectRatio(player.videoSize, view.width, view.height, contentFitEnum)
}
