package com.margelo.nitro.nitrovideo.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.AudioMixingMode
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import java.lang.ref.WeakReference

// ─────────────────────────────────────────────────────────────────────────────
// AudioFocusManager — controls Android AudioManager focus for video players
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
class AudioFocusManager(private val context: Context) : AudioManager.OnAudioFocusChangeListener {

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val players = mutableListOf<WeakReference<HybridVideoPlayer>>()
    private var currentFocusRequest: AudioFocusRequest? = null
    private var currentMixingMode: AudioMixingMode = AudioMixingMode.AUTO

    private val anyPlayerRequiresFocus: Boolean
        get() = players.toList().any { weak ->
            val player = weak.get() ?: return@any false
            (!player.muted && player.playing && player.volume > 0) || player.audioMixingMode == AudioMixingMode.DONOTMIX
        }

    fun registerPlayer(player: HybridVideoPlayer) {
        if (players.none { it.get() == player }) {
            players.add(WeakReference(player))
        }
        updateAudioFocus()
    }

    fun unregisterPlayer(player: HybridVideoPlayer) {
        players.removeAll { it.get() == player || it.get() == null }
        updateAudioFocus()
    }

    fun onPlayerPlaybackChanged() {
        updateAudioFocus()
    }

    fun updateAudioFocus() {
        if (anyPlayerRequiresFocus || findAudioMixingMode() != currentMixingMode) {
            requestAudioFocus()
        } else {
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        val mixingMode = findAudioMixingMode()

        if (mixingMode == AudioMixingMode.MIXWITHOTHERS || !anyPlayerRequiresFocus) {
            abandonAudioFocus()
            currentMixingMode = mixingMode
            return
        }

        val audioFocusType = when (mixingMode) {
            AudioMixingMode.DUCKOTHERS -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            AudioMixingMode.AUTO, AudioMixingMode.DONOTMIX -> AudioManager.AUDIOFOCUS_GAIN
            else -> AudioManager.AUDIOFOCUS_GAIN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentFocusRequest?.let {
                if (it.focusGain == audioFocusType) return
            }

            val request = AudioFocusRequest.Builder(audioFocusType).run {
                setAudioAttributes(
                    AudioAttributes.Builder().run {
                        setUsage(AudioAttributes.USAGE_MEDIA)
                        setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        build()
                    }
                )
                setOnAudioFocusChangeListener(this@AudioFocusManager)
                build()
            }
            currentFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, audioFocusType)
        }
        currentMixingMode = mixingMode
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        currentFocusRequest?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest(it)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(this)
            }
        }
        currentFocusRequest = null
    }

    override fun onAudioFocusChange(focusChange: Int) {
        mainHandler.post {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (findAudioMixingMode() != AudioMixingMode.MIXWITHOTHERS) {
                        players.forEach { weak ->
                            weak.get()?.let { player ->
                                if (!player.muted) player.pause()
                            }
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    val mixing = findAudioMixingMode()
                    players.forEach { weak ->
                        weak.get()?.let { player ->
                            if (mixing == AudioMixingMode.DONOTMIX) {
                                if (!player.muted) player.pause()
                            } else {
                                player.volume /= 2.0
                            }
                        }
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Restore volume after ducking
                    players.forEach { weak ->
                        weak.get()?.let { player ->
                            // Restore full volume if needed
                        }
                    }
                }
            }
        }
    }

    private fun findAudioMixingMode(): AudioMixingMode {
        val activeModes = players.mapNotNull { it.get()?.takeIf { p -> p.playing }?.audioMixingMode }
        return activeModes.firstOrNull() ?: AudioMixingMode.AUTO
    }
}
