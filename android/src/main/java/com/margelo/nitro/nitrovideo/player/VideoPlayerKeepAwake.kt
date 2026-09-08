package com.margelo.nitro.nitrovideo.player

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.managers.VideoManager
import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Keeps the screen on as long as the provided player is playing and some VideoView is visible.
 */
@OptIn(UnstableApi::class)
class VideoPlayerKeepAwake(player: HybridVideoPlayer, enableOnInit: Boolean = true) : ReadWriteProperty<Any?, Boolean> {
    private val videoPlayer = WeakReference(player)
    private val mainHandler = Handler(Looper.getMainLooper())
    var enabled = enableOnInit
        set(value) {
            if (value) {
                enable()
            } else {
                disable()
            }
            field = value
        }
    private var playerListener: Player.Listener? = null

    init {
        if (enableOnInit) {
            enable()
        }
    }

    private fun enable() {
        mainHandler.post {
            val player = this@VideoPlayerKeepAwake.videoPlayer.get() ?: return@post
            val listener = createPlayerListener()
            this@VideoPlayerKeepAwake.playerListener = listener
            player.exoPlayer?.addListener(listener)
            VideoManager.requestKeepAwake(player)
        }
    }

    private fun disable() {
        mainHandler.post {
            val player = this@VideoPlayerKeepAwake.videoPlayer.get() ?: return@post
            val listener = playerListener ?: return@post
            player.exoPlayer?.removeListener(listener)
            this@VideoPlayerKeepAwake.playerListener = null
            VideoManager.releaseKeepAwake(player)
        }
    }

    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val player = this@VideoPlayerKeepAwake.videoPlayer.get() ?: return
                if (isPlaying) {
                    VideoManager.requestKeepAwake(player)
                } else {
                    VideoManager.releaseKeepAwake(player)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val player = this@VideoPlayerKeepAwake.videoPlayer.get() ?: return
                when (playbackState) {
                    Player.STATE_READY, Player.STATE_BUFFERING -> {
                        if (player.exoPlayer?.playWhenReady == true) {
                            VideoManager.requestKeepAwake(player)
                        }
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        VideoManager.releaseKeepAwake(player)
                    }
                }
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = this.enabled

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        this.enabled = value
    }
}
