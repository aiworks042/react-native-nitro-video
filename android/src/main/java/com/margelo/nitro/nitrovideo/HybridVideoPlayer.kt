package com.margelo.nitro.nitrovideo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import com.margelo.nitro.nitrovideo.delegates.IgnoreSameSet
import com.margelo.nitro.nitrovideo.enums.AudioMixingMode
import com.margelo.nitro.nitrovideo.enums.ContentType
import com.margelo.nitro.nitrovideo.enums.PlayerStatus
import com.margelo.nitro.nitrovideo.listeners.VideoPlayerListener
import com.margelo.nitro.nitrovideo.managers.AudioFocusManager
import com.margelo.nitro.nitrovideo.managers.VideoManager
import com.margelo.nitro.nitrovideo.playbackService.NitroVideoPlaybackService
import com.margelo.nitro.nitrovideo.playbackService.PlaybackServiceConnection
import com.margelo.nitro.nitrovideo.player.FirstFrameEventGenerator
import com.margelo.nitro.nitrovideo.player.PlayerEvent
import com.margelo.nitro.nitrovideo.player.VideoPlayerKeepAwake
import com.margelo.nitro.nitrovideo.player.VideoPlayerLoadControl
import com.margelo.nitro.nitrovideo.player.VideoPlayerTracksHelper
import com.margelo.nitro.nitrovideo.records.BufferOptions
import com.margelo.nitro.nitrovideo.records.PlaybackError
import com.margelo.nitro.nitrovideo.records.ScrubbingModeOptions
import com.margelo.nitro.nitrovideo.records.SeekTolerance
import com.margelo.nitro.nitrovideo.records.VideoSource
import com.margelo.nitro.nitrovideo.records.VideoThumbnailOptions
import com.margelo.nitro.nitrovideo.utils.MutableWeakReference
import com.margelo.nitro.nitrovideo.utils.buildBasicMediaSession
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// ─────────────────────────────────────────────────────────────────────────────
// NitroVideoPlayerRegistry — thread-safe player registry
// ─────────────────────────────────────────────────────────────────────────────

object NitroVideoPlayerRegistry {
    private val players = ConcurrentHashMap<Int, HybridVideoPlayer>()

    fun register(id: Int, player: HybridVideoPlayer) {
        players[id] = player
    }

    fun unregister(id: Int) {
        players.remove(id)
    }

    fun get(id: Int): HybridVideoPlayer? = players[id]
}

// ─────────────────────────────────────────────────────────────────────────────
// HybridVideoPlayer — Android ExoPlayer (Media3) backing VideoPlayer
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
class HybridVideoPlayer : HybridVideoPlayerSpec(), IntervalUpdateEmitter {

    companion object {
        private val nextId = AtomicInteger(1)
        private var sharedAudioFocusManager: AudioFocusManager? = null

        @Synchronized
        fun getAudioFocusManager(context: Context): AudioFocusManager {
            if (sharedAudioFocusManager == null) {
                sharedAudioFocusManager = AudioFocusManager(context.applicationContext)
            }
            return sharedAudioFocusManager!!
        }
    }

    override val id: Double = nextId.getAndIncrement().toDouble()

    private val context: Context
        get() = NitroModules.applicationContext
            ?: throw IllegalStateException("NitroModules.applicationContext is null!")

    private val mainHandler = Handler(Looper.getMainLooper())
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() = _exoPlayer ?: throw IllegalStateException("Player has been released")

    var tracksHelper: VideoPlayerTracksHelper? = null
        private set
    private val audioFocusManager: AudioFocusManager by lazy {
        getAudioFocusManager(context)
    }

    val loadControl = VideoPlayerLoadControl()
    var mediaSession: MediaSession? = null
    val serviceConnection by lazy { PlaybackServiceConnection(WeakReference(this)) }
    val intervalUpdateClock = IntervalUpdateClock(this)

    internal val currentVideoViewRef = MutableWeakReference<HybridNitroVideoView?>(null) { new, old ->
        sendEvent(PlayerEvent.TargetViewChanged(new, old))
    }
    internal var firstFrameEventGenerator: FirstFrameEventGenerator? = null

    // ─── Native & JS Event system ─────────────────────────────────────────────

    private val nativeListeners = mutableListOf<WeakReference<VideoPlayerListener>>()
    private val listenerIdCounter = AtomicInteger(0)
    private val jsListeners = ConcurrentHashMap<Int, Pair<String, (String) -> Unit>>()

    // ─── State ───────────────────────────────────────────────────────────────

    var currentSource: VideoSource? = null

    override var loop: Boolean = false
        set(value) {
            field = value
            _exoPlayer?.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }

    override var allowsExternalPlayback: Boolean = true

    override var audioMixingMode: com.margelo.nitro.nitrovideo.AudioMixingMode =
        com.margelo.nitro.nitrovideo.AudioMixingMode.AUTO
        set(value) {
            field = value
            val domainMode = when (value) {
                com.margelo.nitro.nitrovideo.AudioMixingMode.MIXWITHOTHERS -> AudioMixingMode.MIX_WITH_OTHERS
                com.margelo.nitro.nitrovideo.AudioMixingMode.DUCKOTHERS -> AudioMixingMode.DUCK_OTHERS
                com.margelo.nitro.nitrovideo.AudioMixingMode.AUTO -> AudioMixingMode.AUTO
                com.margelo.nitro.nitrovideo.AudioMixingMode.DONOTMIX -> AudioMixingMode.DO_NOT_MIX
            }
            sendEvent(PlayerEvent.AudioMixingModeChanged(domainMode, null))
            audioFocusManager.onPlayerPlaybackChanged()
        }

    override var muted: Boolean
        get() = (_exoPlayer?.volume ?: 1f) == 0f
        set(value) {
            val oldMuted = muted
            _exoPlayer?.volume = if (value) 0f else userVolume
            sendEvent(PlayerEvent.MutedChanged(value, oldMuted))
            audioFocusManager.onPlayerPlaybackChanged()
        }

    override var currentTime: Double
        get() = (_exoPlayer?.currentPosition?.toDouble() ?: 0.0) / 1000.0
        set(value) {
            _exoPlayer?.seekTo((value * 1000).toLong())
        }

    var userVolume: Float = 1f

    override var volume: Double
        get() = (_exoPlayer?.volume ?: 1f).toDouble()
        set(value) {
            val oldVolume = (_exoPlayer?.volume ?: 1f)
            userVolume = value.toFloat()
            if (!muted) {
                _exoPlayer?.volume = userVolume
            }
            sendEvent(PlayerEvent.VolumeChanged(userVolume, oldVolume))
            audioFocusManager.onPlayerPlaybackChanged()
        }

    override var preservesPitch: Boolean = true
        set(value) {
            field = value
            val current = _exoPlayer?.playbackParameters ?: PlaybackParameters.DEFAULT
            _exoPlayer?.playbackParameters = PlaybackParameters(current.speed, if (value) 1f else current.speed)
        }

    override var timeUpdateEventInterval: Double = 0.0
        set(value) {
            field = value
            intervalUpdateClock.interval = (value * 1000).toLong()
        }

    override var playbackRate: Double
        get() = (_exoPlayer?.playbackParameters?.speed ?: 1f).toDouble()
        set(value) {
            val oldSpeed = _exoPlayer?.playbackParameters?.speed ?: 1f
            _exoPlayer?.playbackParameters = PlaybackParameters(value.toFloat())
            sendEvent(PlayerEvent.PlaybackRateChanged(value.toFloat(), oldSpeed))
        }

    override var keepScreenOnWhilePlaying: Boolean by VideoPlayerKeepAwake(this)

    override var showNowPlayingNotification: Boolean = false
        set(value) {
            field = value
            if (value) {
                startPlaybackService()
            }
            serviceConnection.playbackServiceBinder?.service?.setShowNotification(value, exoPlayer)
        }

    override var staysActiveInBackground: Boolean = false
        set(value) {
            field = value
            if (value) {
                startPlaybackService()
            }
        }

    override var status: VideoPlayerStatus = VideoPlayerStatus.IDLE

    // ─── Init ────────────────────────────────────────────────────────────────

    init {
        NitroVideoPlayerRegistry.register(id.toInt(), this)
        VideoManager.registerVideoPlayer(this)
        mainHandler.post { buildPlayer() }
    }

    private fun buildPlayer() {
        val renderersFactory = DefaultRenderersFactory(context)
            .forceEnableMediaCodecAsynchronousQueueing()
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(context)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, false)
            .build()

        tracksHelper = VideoPlayerTracksHelper(player)
        player.addListener(playerListener)
        mediaSession = buildBasicMediaSession(context, player)
        firstFrameEventGenerator = FirstFrameEventGenerator(this, currentVideoViewRef) {
            sendEvent(PlayerEvent.RenderedFirstFrame())
        }

        _exoPlayer = player
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val newStatus = when (playbackState) {
                Player.STATE_IDLE -> VideoPlayerStatus.IDLE
                Player.STATE_BUFFERING -> VideoPlayerStatus.LOADING
                Player.STATE_READY -> VideoPlayerStatus.READYTOPLAY
                Player.STATE_ENDED -> VideoPlayerStatus.IDLE
                else -> VideoPlayerStatus.IDLE
            }
            status = newStatus

            val domainStatus = when (newStatus) {
                VideoPlayerStatus.IDLE -> PlayerStatus.IDLE
                VideoPlayerStatus.LOADING -> PlayerStatus.LOADING
                VideoPlayerStatus.READYTOPLAY -> PlayerStatus.READY_TO_PLAY
                VideoPlayerStatus.ERROR -> PlayerStatus.ERROR
            }
            val error = if (playbackState == Player.STATE_ENDED && _exoPlayer?.playerError != null) {
                PlaybackError(_exoPlayer!!.playerError!!)
            } else null

            sendEvent(PlayerEvent.StatusChanged(domainStatus, null, error))

            if (playbackState == Player.STATE_ENDED && error == null) {
                sendEvent(PlayerEvent.PlayedToEnd())
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            sendEvent(PlayerEvent.IsPlayingChanged(isPlaying, !isPlaying))
            audioFocusManager.onPlayerPlaybackChanged()
        }

        override fun onPlaybackParametersChanged(parameters: PlaybackParameters) {
            sendEvent(PlayerEvent.PlaybackRateChanged(parameters.speed, null))
        }

        override fun onVolumeChanged(vol: Float) {
            if (!muted) {
                sendEvent(PlayerEvent.VolumeChanged(vol, null))
            }
            audioFocusManager.onPlayerPlaybackChanged()
        }

        override fun onPlayerError(error: PlaybackException) {
            status = VideoPlayerStatus.ERROR
            sendEvent(PlayerEvent.StatusChanged(PlayerStatus.ERROR, null, PlaybackError(error)))
        }

        override fun onTracksChanged(tracks: Tracks) {
            tracksHelper?.onTracksChanged(tracks)
            sendEvent(PlayerEvent.TracksChanged(tracks))

            val dur = if ((_exoPlayer?.duration ?: C.TIME_UNSET) != C.TIME_UNSET) {
                (_exoPlayer?.duration?.toDouble() ?: 0.0) / 1000.0
            } else {
                0.0
            }
            sendEvent(
                PlayerEvent.VideoSourceLoaded(
                    currentSource,
                    dur,
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            )
        }
    }

    // ─── Readonly state ───────────────────────────────────────────────────────

    override val playing: Boolean get() = _exoPlayer?.isPlaying ?: false
    override val duration: Double
        get() = (_exoPlayer?.duration?.takeIf { it != C.TIME_UNSET }?.toDouble() ?: 0.0) / 1000.0
    override val isLive: Boolean get() = _exoPlayer?.isCurrentMediaItemLive ?: false
    override val bufferedPosition: Double
        get() = (_exoPlayer?.bufferedPosition?.toDouble() ?: 0.0) / 1000.0
    override val isExternalPlaybackActive: Boolean get() = false

    // ─── Playback methods ─────────────────────────────────────────────────────

    override fun load(uri: String, useCaching: Boolean, contentType: String) {
        if (uri.isEmpty()) return
        mainHandler.post {
            val videoSource = VideoSource(
                uri = Uri.parse(uri),
                useCaching = useCaching,
                contentType = ContentType.fromString(contentType)
            )
            currentSource = videoSource
            sendEvent(PlayerEvent.SourceChanged(videoSource, null))

            val mediaSource = videoSource.toMediaSource(context)
            if (mediaSource != null) {
                _exoPlayer?.setMediaSource(mediaSource)
                _exoPlayer?.prepare()
                _exoPlayer?.playWhenReady = true
            } else {
                val mediaItem = MediaItem.fromUri(Uri.parse(uri))
                _exoPlayer?.setMediaItem(mediaItem)
                _exoPlayer?.prepare()
                _exoPlayer?.playWhenReady = true
            }
        }
    }

    override fun play() {
        mainHandler.post { _exoPlayer?.play() }
    }

    override fun pause() {
        mainHandler.post { _exoPlayer?.pause() }
    }

    override fun replaceAsync(uri: String): Promise<Unit> {
        val promise = Promise.async<Unit>()
        mainHandler.post {
            try {
                load(uri, false, "auto")
                promise.resolve(Unit)
            } catch (e: Exception) {
                promise.reject(e)
            }
        }
        return promise
    }

    override fun seekBy(seconds: Double) {
        mainHandler.post {
            val currentMs = _exoPlayer?.currentPosition ?: 0L
            _exoPlayer?.seekTo(currentMs + (seconds * 1000).toLong())
        }
    }

    override fun replay() {
        mainHandler.post {
            _exoPlayer?.seekTo(0)
            _exoPlayer?.play()
        }
    }

    override fun setBufferOptions(options: NitroBufferOptions) {
        mainHandler.post {
            val bo = BufferOptions(
                preferredForwardBufferDuration = options.preferredForwardBufferDuration,
                maxBufferBytes = (options.maxBufferBytes ?: 0.0).toLong(),
                prioritizeTimeOverSizeThreshold = options.prioritizeTimeOverSizeThreshold ?: false,
                minBufferForPlayback = options.minBufferForPlayback ?: 1.0
            )
            loadControl.applyBufferOptions(bo)
        }
    }

    override fun setSeekTolerance(toleranceBefore: Double, toleranceAfter: Double) {
        mainHandler.post {
            _exoPlayer?.let {
                SeekTolerance(toleranceBefore, toleranceAfter).applyToPlayer(it)
            }
        }
    }

    override fun setScrubbingModeOptions(options: NitroScrubbingModeOptions) {
        mainHandler.post {
            _exoPlayer?.let {
                val smo = ScrubbingModeOptions(
                    scrubbingModeEnabled = options.scrubbingModeEnabled ?: false,
                    increaseCodecOperatingRate = options.increaseCodecOperatingRate ?: true,
                    enableDynamicScheduling = options.enableDynamicScheduling ?: true,
                    useDecodeOnlyFlag = options.useDecodeOnlyFlag ?: true,
                    allowSkippingMediaCodecFlush = options.allowSkippingMediaCodecFlush ?: true
                )
                smo.applyToPlayer(it)
            }
        }
    }

    override fun setSubtitleTrack(trackId: String) {
        mainHandler.post {
            tracksHelper?.selectSubtitleTrack(trackId)
        }
    }

    override fun setAudioTrack(trackId: String) {
        mainHandler.post {
            tracksHelper?.selectAudioTrack(trackId)
        }
    }

    override fun setMaxResolution(width: Double, height: Double) {
        mainHandler.post {
            tracksHelper?.setMaxResolution(width.toInt(), height.toInt())
        }
    }

    override fun generateThumbnailsAsync(
        times: DoubleArray,
        maxWidth: Double,
        maxHeight: Double
    ): Promise<Array<NitroVideoThumbnail>> {
        val promise = Promise.async<Array<NitroVideoThumbnail>>()
        Thread {
            try {
                val source = currentSource
                val uri = source?.uri ?: throw IllegalStateException("Video source is not set")
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val results = mutableListOf<NitroVideoThumbnail>()
                val opt = VideoThumbnailOptions(maxWidth.toInt(), maxHeight.toInt())

                for (sec in times) {
                    val thumbResult = retriever.generateThumbnailAtTime(sec.seconds, opt)
                    val baos = ByteArrayOutputStream()
                    thumbResult.bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    results.add(
                        NitroVideoThumbnail(
                            actualTime = thumbResult.actualTime.inWholeMilliseconds.toDouble() / 1000.0,
                            requestedTime = sec,
                            width = thumbResult.width.toDouble(),
                            height = thumbResult.height.toDouble(),
                            uri = "data:image/jpeg;base64,$base64"
                        )
                    )
                }
                retriever.close()
                promise.resolve(results.toTypedArray())
            } catch (e: Exception) {
                promise.reject(e)
            }
        }.start()
        return promise
    }

    private fun startPlaybackService(): Boolean {
        if (serviceConnection.playbackServiceBinder?.service != null) return true
        return NitroVideoPlaybackService.startService(context, serviceConnection)
    }

    // ─── Event system ─────────────────────────────────────────────────────────

    fun addListener(listener: VideoPlayerListener) {
        synchronized(nativeListeners) {
            if (nativeListeners.none { it.get() == listener }) {
                nativeListeners.add(WeakReference(listener))
            }
        }
    }

    fun removeListener(listener: VideoPlayerListener) {
        synchronized(nativeListeners) {
            nativeListeners.removeAll { it.get() == listener }
        }
    }

    private fun sendEvent(event: PlayerEvent) {
        val snapshot = synchronized(nativeListeners) {
            nativeListeners.mapNotNull { it.get() }
        }
        event.emit(this, snapshot)

        if (event.emitToJS) {
            val json = when (event) {
                is PlayerEvent.StatusChanged ->
                    "{\"status\":\"${event.status.value}\"${if (event.error != null) ",\"error\":{\"message\":\"${event.error.message}\"}}" else "}"}"
                is PlayerEvent.IsPlayingChanged -> "{\"isPlaying\":${event.isPlaying}}"
                is PlayerEvent.VolumeChanged -> "{\"volume\":${event.volume}}"
                is PlayerEvent.MutedChanged -> "{\"muted\":${event.muted}}"
                is PlayerEvent.PlaybackRateChanged -> "{\"playbackRate\":${event.rate}}"
                is PlayerEvent.TimeUpdated ->
                    "{\"currentTime\":${event.timeUpdate.currentTime},\"bufferedPosition\":${event.timeUpdate.bufferedPosition}}"
                is PlayerEvent.VideoSourceLoaded -> "{\"duration\":${event.duration}}"
                is PlayerEvent.PlayedToEnd -> "{}"
                else -> "{}"
            }
            emit(event.name, json)
        }
    }

    override fun addListener(event: String, callback: (String) -> Unit): Double {
        val listenerId = listenerIdCounter.incrementAndGet()
        jsListeners[listenerId] = Pair(event, callback)
        return listenerId.toDouble()
    }

    override fun removeListener(listenerId: Double) {
        jsListeners.remove(listenerId.toInt())
    }

    private fun emit(event: String, json: String) {
        mainHandler.post {
            jsListeners.values
                .filter { it.first == event }
                .forEach { (_, callback) -> callback(json) }
        }
    }

    // ─── IntervalUpdateEmitter ───────────────────────────────────────────────

    override fun emitTimeUpdate() {
        val current = (_exoPlayer?.currentPosition?.toDouble() ?: 0.0) / 1000.0
        val buffered = (_exoPlayer?.bufferedPosition?.toDouble() ?: 0.0) / 1000.0
        val update = com.margelo.nitro.nitrovideo.records.TimeUpdate(
            currentTime = current,
            bufferedPosition = buffered
        )
        sendEvent(PlayerEvent.TimeUpdated(update))
    }

    // ─── Release ─────────────────────────────────────────────────────────────

    fun release() {
        NitroVideoPlayerRegistry.unregister(id.toInt())
        VideoManager.unregisterVideoPlayer(this)
        intervalUpdateClock.interval = 0L

        if (serviceConnection.isConnected) {
            try {
                context.unbindService(serviceConnection)
            } catch (_: Exception) {}
        }
        serviceConnection.playbackServiceBinder?.service?.unregisterPlayer(exoPlayer)
        mediaSession?.release()

        mainHandler.post {
            firstFrameEventGenerator?.release()
            _exoPlayer?.removeListener(playerListener)
            _exoPlayer?.release()
            _exoPlayer = null
        }
        jsListeners.clear()
        synchronized(nativeListeners) { nativeListeners.clear() }
    }
}
