package com.margelo.nitro.nitrovideo.views

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.VideoContentFit
import java.util.WeakHashMap

@OptIn(UnstableApi::class)
class HybridNitroVideoViewManager : SimpleViewManager<PlayerView>() {

    override fun getName(): String = "NitroVideoView"

    private val viewMap = WeakHashMap<PlayerView, HybridNitroVideoView>()

    override fun createViewInstance(reactContext: ThemedReactContext): PlayerView {
        val hybridView = HybridNitroVideoView()
        val playerView = hybridView.playerView
        viewMap[playerView] = hybridView
        return playerView
    }

    override fun onDropViewInstance(view: PlayerView) {
        super.onDropViewInstance(view)
        viewMap[view]?.onDropView()
        viewMap.remove(view)
    }

    @ReactProp(name = "playerId")
    fun setPlayerId(view: PlayerView, playerId: Double?) {
        viewMap[view]?.playerId = playerId
    }

    @ReactProp(name = "nativeControls")
    fun setNativeControls(view: PlayerView, nativeControls: Boolean) {
        viewMap[view]?.nativeControls = nativeControls
    }

    @ReactProp(name = "contentFit")
    fun setContentFit(view: PlayerView, contentFit: String?) {
        val fit = when (contentFit) {
            "cover" -> VideoContentFit.COVER
            "fill" -> VideoContentFit.FILL
            else -> VideoContentFit.CONTAIN
        }
        viewMap[view]?.contentFit = fit
    }

    @ReactProp(name = "allowsPictureInPicture")
    fun setAllowsPictureInPicture(view: PlayerView, allows: Boolean) {
        viewMap[view]?.allowsPictureInPicture = allows
    }

    @ReactProp(name = "startsPictureInPictureAutomatically")
    fun setStartsPictureInPictureAutomatically(view: PlayerView, auto: Boolean) {
        viewMap[view]?.startsPictureInPictureAutomatically = auto
    }

    @ReactProp(name = "requiresLinearPlayback")
    fun setRequiresLinearPlayback(view: PlayerView, linear: Boolean) {
        viewMap[view]?.requiresLinearPlayback = linear
    }

    @ReactProp(name = "useExoShutter")
    fun setUseExoShutter(view: PlayerView, shutter: Boolean) {
        viewMap[view]?.useExoShutter = shutter
    }

    @ReactProp(name = "controllerAutoShow")
    fun setControllerAutoShow(view: PlayerView, autoShow: Boolean) {
        viewMap[view]?.controllerAutoShow = autoShow
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any>? {
        return MapBuilder.builder<String, Any>()
            .put("onPictureInPictureStart", MapBuilder.of("registrationName", "onPictureInPictureStart"))
            .put("onPictureInPictureStop", MapBuilder.of("registrationName", "onPictureInPictureStop"))
            .put("onFullscreenEnter", MapBuilder.of("registrationName", "onFullscreenEnter"))
            .put("onFullscreenExit", MapBuilder.of("registrationName", "onFullscreenExit"))
            .put("onFirstFrameRender", MapBuilder.of("registrationName", "onFirstFrameRender"))
            .build()
            .toMutableMap()
    }

    override fun receiveCommand(root: PlayerView, commandId: String, args: ReadableArray?) {
        val hybridView = viewMap[root] ?: return
        when (commandId) {
            "enterFullscreen" -> hybridView.enterFullscreen()
            "exitFullscreen" -> hybridView.exitFullscreen()
            "startPictureInPicture" -> hybridView.startPictureInPicture()
            "stopPictureInPicture" -> hybridView.stopPictureInPicture()
        }
    }
}
