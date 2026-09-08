package com.margelo.nitro.nitrovideo.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.listeners.VideoPlayerListener
import com.margelo.nitro.nitrovideo.records.AudioTrack
import java.lang.ref.WeakReference

@OptIn(UnstableApi::class)
class VideoPlayerAudioTracks(owner: HybridVideoPlayer) : VideoPlayerListener {
    private val owner = WeakReference(owner)
    private val videoPlayer: HybridVideoPlayer?
        get() = owner.get()
    private val formatsToGroups = mutableMapOf<Format, Pair<TrackGroup, Int>>()
    private var currentAudioTrackFormat: Format? = null
    private var currentOverride: TrackSelectionOverride? = null

    var currentAudioTrack: AudioTrack?
        get() = AudioTrack.fromFormat(currentAudioTrackFormat)
        set(value) {
            applyAudioTrack(value)
        }
    val availableAudioTracks = arrayListOf<AudioTrack>()

    init {
        owner.addListener(this)
    }

    fun setAudioTracksEnabled(enabled: Boolean) {
        val currentParams = videoPlayer?.exoPlayer?.trackSelectionParameters ?: return
        var params = currentParams.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !enabled).build()
        if (!enabled) {
            params = params.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()
        }
        videoPlayer?.exoPlayer?.trackSelectionParameters = params
    }

    override fun onTrackSelectionParametersChanged(player: HybridVideoPlayer, trackSelectionParameters: TrackSelectionParameters) {
        currentAudioTrackFormat = findSelectedAudioFormat()
        super.onTrackSelectionParametersChanged(player, trackSelectionParameters)
    }

    override fun onTracksChanged(player: HybridVideoPlayer, tracks: Tracks) {
        formatsToGroups.clear()
        availableAudioTracks.clear()
        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                val format: Format = group.getTrackFormat(i)
                if (MimeTypes.isAudio(format.sampleMimeType)) {
                    formatsToGroups[format] = group.mediaTrackGroup to i
                    val track = AudioTrack.fromFormat(format) ?: continue
                    availableAudioTracks.add(track)
                }
            }
        }
        currentAudioTrackFormat = findSelectedAudioFormat()
        super.onTracksChanged(player, tracks)
    }

    private fun applyAudioTrack(audioTrack: AudioTrack?) {
        val player = videoPlayer?.exoPlayer ?: return
        var newParameters: TrackSelectionParameters = player.trackSelectionParameters
        currentOverride?.let {
            newParameters = newParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()
        }
        if (audioTrack == null) {
            player.trackSelectionParameters = newParameters
            setAudioTracksEnabled(false)
            currentOverride = null
            return
        }
        val format = formatsToGroups.keys.firstOrNull { it.id == audioTrack.id }
        format?.let {
            formatsToGroups[it]?.let { audioPair ->
                val trackSelectionOverride = TrackSelectionOverride(audioPair.first, audioPair.second)
                newParameters = newParameters.buildUpon().addOverride(trackSelectionOverride).build()
                player.trackSelectionParameters = newParameters
                setAudioTracksEnabled(true)
                currentOverride = trackSelectionOverride
            }
        }
    }

    private fun findSelectedAudioFormat(): Format? {
        val trackSelectionParameters = videoPlayer?.exoPlayer?.trackSelectionParameters
        val preferredAudioLanguages = trackSelectionParameters?.preferredAudioLanguages
        val overriddenFormat: Format? = trackSelectionParameters?.overrides?.let {
            for ((group, trackSelectionOverride) in it) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    return@let trackSelectionOverride.trackIndices.firstOrNull()?.let { index ->
                        group.getFormat(index)
                    }
                }
            }
            return@let null
        }

        val preferredFormat: Format? = preferredAudioLanguages?.let { languages ->
            for (preferredLanguage in languages) {
                return@let formatsToGroups.keys.firstOrNull {
                    it.language == preferredLanguage
                }
            }
            return@let null
        }

        return overriddenFormat ?: preferredFormat
    }
}
