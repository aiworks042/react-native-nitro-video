package com.margelo.nitro.nitrovideo

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext
import com.margelo.nitro.views.RecyclableView

@Keep
@DoNotStrip
@OptIn(UnstableApi::class)
class HybridNitroVideoView(
  val context: ThemedReactContext,
) : HybridNitroVideoViewSpec(), RecyclableView {

  companion object {
    private const val TAG = "HybridNitroVideoView"
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private val playerView = PlayerView(context).apply {
    useController = false
    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
  }

  override val view: View = playerView

  private var player: ExoPlayer? = null
  private var isLoaded = false
  private var lastLoadedSource: String = ""
  private var progressRunnable: Runnable? = null

  // Properties
  override var source: String = ""
    set(value) {
      field = value
      if (Looper.myLooper() == Looper.getMainLooper()) {
        checkAndLoadSource()
      } else {
        mainHandler.post { checkAndLoadSource() }
      }
    }

  override var paused: Boolean? = false
    set(value) {
      field = value
      val shouldPlay = !(value ?: false)
      mainHandler.post {
        player?.let {
          if (it.playWhenReady != shouldPlay) {
            it.playWhenReady = shouldPlay
          }
        }
      }
    }

  override var muted: Boolean? = false
    set(value) {
      field = value
      mainHandler.post { updateVolume() }
    }

  override var repeat: Boolean? = false
    set(value) {
      field = value
      mainHandler.post {
        player?.repeatMode = if (value == true) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
      }
    }

  override var volume: Double? = 1.0
    set(value) {
      field = value
      mainHandler.post { updateVolume() }
    }

  override var resizeMode: ResizeMode? = ResizeMode.CONTAIN
    set(value) {
      field = value
      mainHandler.post { updateResizeMode() }
    }

  override var onLoad: ((duration: Double) -> Unit)? = null
  override var onProgress: ((currentTime: Double, duration: Double) -> Unit)? = null
  override var onEnd: (() -> Unit)? = null
  override var onError: ((error: String) -> Unit)? = null

  // Methods
  override fun play() {
    mainHandler.post {
      paused = false
      player?.playWhenReady = true
      startProgressTracking()
    }
  }

  override fun pause() {
    mainHandler.post {
      paused = true
      player?.playWhenReady = false
      stopProgressTracking()
    }
  }

  override fun seek(position: Double) {
    mainHandler.post {
      val positionMs = (position * 1000).toLong()
      player?.seekTo(positionMs)
    }
  }

  // Lifecycle
  override fun afterUpdate() {
    super.afterUpdate()
    mainHandler.post {
      applyAllProps()
    }
  }

  override fun onDropView() {
    super.onDropView()
    mainHandler.post {
      stopProgressTracking()
      player?.let {
        it.stop()
        it.clearMediaItems()
        it.release()
      }
      player = null
      playerView.player = null
      isLoaded = false
      lastLoadedSource = ""
    }
  }

  override fun prepareForRecycle() {
    mainHandler.post {
      stopProgressTracking()
      player?.let {
        it.stop()
        it.clearMediaItems()
      }
      isLoaded = false
      lastLoadedSource = ""
      source = ""
      paused = false
      muted = false
      repeat = false
      volume = 1.0
      resizeMode = ResizeMode.CONTAIN
      updateResizeMode()
    }
  }

  private fun getOrCreatePlayer(): ExoPlayer {
    player?.let { return it }

    val newPlayer = ExoPlayer.Builder(context).build().apply {
      repeatMode = if (repeat == true) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
      val targetVolume = if (muted == true) 0f else (this@HybridNitroVideoView.volume?.toFloat() ?: 1.0f).coerceIn(0f, 1f)
      volume = targetVolume
      playWhenReady = !(paused ?: false)

      addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
          when (playbackState) {
            Player.STATE_READY -> {
              val durationSec = duration.toDouble() / 1000.0
              if (durationSec > 0 && !isLoaded) {
                isLoaded = true
                onLoad?.invoke(durationSec)
              }
              if (playWhenReady) {
                startProgressTracking()
              }
            }
            Player.STATE_ENDED -> {
              stopProgressTracking()
              onEnd?.invoke()
            }
            Player.STATE_BUFFERING -> {}
            Player.STATE_IDLE -> {
              stopProgressTracking()
            }
          }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
          if (isPlaying) {
            startProgressTracking()
          } else {
            stopProgressTracking()
          }
        }

        override fun onPlayerError(error: PlaybackException) {
          stopProgressTracking()
          Log.e(TAG, "ExoPlayer playback error: ${error.message}", error)
          onError?.invoke(error.message ?: "Playback error")
        }
      })
    }

    player = newPlayer
    playerView.player = newPlayer
    return newPlayer
  }

  private fun applyAllProps() {
    val p = getOrCreatePlayer()
    updateResizeMode()
    p.repeatMode = if (repeat == true) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    updateVolume()
    checkAndLoadSource()
    val shouldPlay = !(paused ?: false)
    if (p.playWhenReady != shouldPlay) {
      p.playWhenReady = shouldPlay
    }
  }

  private fun updateResizeMode() {
    playerView.resizeMode = when (resizeMode) {
      ResizeMode.COVER -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
      ResizeMode.CONTAIN -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      ResizeMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      null -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }
  }

  private fun updateVolume() {
    val targetVolume = if (muted == true) 0f else (volume?.toFloat() ?: 1.0f).coerceIn(0f, 1f)
    player?.volume = targetVolume
  }

  private fun checkAndLoadSource() {
    val currentSource = source.trim()
    if (currentSource.isEmpty()) return
    if (currentSource == lastLoadedSource && isLoaded) return

    val p = getOrCreatePlayer()
    lastLoadedSource = currentSource
    isLoaded = false

    try {
      val uri = Uri.parse(currentSource)
      val mediaItem = MediaItem.fromUri(uri)
      p.setMediaItem(mediaItem)
      p.prepare()
      p.playWhenReady = !(paused ?: false)
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to load video URI: $currentSource", e)
      onError?.invoke("Failed to load video: ${e.message}")
    }
  }

  private fun startProgressTracking() {
    stopProgressTracking()
    val runnable = object : Runnable {
      override fun run() {
        val p = player
        if (p != null && p.isPlaying) {
          val currentSec = (p.currentPosition.coerceAtLeast(0L)).toDouble() / 1000.0
          val durationSec = (p.duration.coerceAtLeast(0L)).toDouble() / 1000.0
          onProgress?.invoke(currentSec, durationSec)
          mainHandler.postDelayed(this, 250)
        }
      }
    }
    progressRunnable = runnable
    mainHandler.postDelayed(runnable, 250)
  }

  private fun stopProgressTracking() {
    progressRunnable?.let {
      mainHandler.removeCallbacks(it)
      progressRunnable = null
    }
  }
}
