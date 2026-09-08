package com.margelo.nitro.nitrovideo.player

import androidx.annotation.OptIn
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.enums.AudioMixingMode
import com.margelo.nitro.nitrovideo.enums.PlayerStatus
import com.margelo.nitro.nitrovideo.listeners.VideoPlayerListener
import com.margelo.nitro.nitrovideo.records.AudioTrack
import com.margelo.nitro.nitrovideo.records.AudioTrackChangedEventPayload
import com.margelo.nitro.nitrovideo.records.AvailableAudioTracksChangedEventPayload
import com.margelo.nitro.nitrovideo.records.AvailableSubtitleTracksChangedEventPayload
import com.margelo.nitro.nitrovideo.records.IsPlayingEventPayload
import com.margelo.nitro.nitrovideo.records.MutedChangedEventPayload
import com.margelo.nitro.nitrovideo.records.PlaybackError
import com.margelo.nitro.nitrovideo.records.PlaybackRateChangedEventPayload
import com.margelo.nitro.nitrovideo.records.SourceChangedEventPayload
import com.margelo.nitro.nitrovideo.records.StatusChangedEventPayload
import com.margelo.nitro.nitrovideo.records.SubtitleTrack
import com.margelo.nitro.nitrovideo.records.SubtitleTrackChangedEventPayload
import com.margelo.nitro.nitrovideo.records.TimeUpdate
import com.margelo.nitro.nitrovideo.records.VideoEventPayload
import com.margelo.nitro.nitrovideo.records.VideoSource
import com.margelo.nitro.nitrovideo.records.VideoSourceLoadedEventPayload
import com.margelo.nitro.nitrovideo.records.VideoTrack
import com.margelo.nitro.nitrovideo.records.VideoTrackChangedEventPayload
import com.margelo.nitro.nitrovideo.records.VolumeChangedEventPayload

@OptIn(UnstableApi::class)
sealed class PlayerEvent {
    open val name: String = ""
    open val jsEventPayload: VideoEventPayload? = null
    open val emitToJS: Boolean = true

    data class StatusChanged(val status: PlayerStatus, val oldStatus: PlayerStatus?, val error: PlaybackError?) : PlayerEvent() {
        override val name = "statusChange"
        override val jsEventPayload = StatusChangedEventPayload(status, oldStatus, error)
    }

    data class IsPlayingChanged(val isPlaying: Boolean, val oldIsPlaying: Boolean?) : PlayerEvent() {
        override val name = "playingChange"
        override val jsEventPayload = IsPlayingEventPayload(isPlaying, oldIsPlaying)
    }

    data class VolumeChanged(val volume: Float, val oldVolume: Float?) : PlayerEvent() {
        override val name = "volumeChange"
        override val jsEventPayload = VolumeChangedEventPayload(volume, oldVolume)
    }

    data class MutedChanged(val muted: Boolean, val oldMuted: Boolean?) : PlayerEvent() {
        override val name = "mutedChange"
        override val jsEventPayload = MutedChangedEventPayload(muted, oldMuted)
    }

    data class SourceChanged(val source: VideoSource?, val oldSource: VideoSource?) : PlayerEvent() {
        override val name = "sourceChange"
        override val jsEventPayload = SourceChangedEventPayload(source, oldSource)
    }

    data class PlaybackRateChanged(val rate: Float, val oldRate: Float?) : PlayerEvent() {
        override val name = "playbackRateChange"
        override val jsEventPayload = PlaybackRateChangedEventPayload(rate, oldRate)
    }

    data class TracksChanged(val tracks: Tracks) : PlayerEvent() {
        override val name = "tracksChange"
        override val emitToJS = false
    }

    data class TrackSelectionParametersChanged(val trackSelectionParameters: TrackSelectionParameters) : PlayerEvent() {
        override val name = "trackSelectionParametersChange"
        override val emitToJS = false
    }

    data class SubtitleTrackChanged(val subtitleTrack: SubtitleTrack?, val oldSubtitleTrack: SubtitleTrack?) : PlayerEvent() {
        override val name = "subtitleTrackChange"
        override val jsEventPayload = SubtitleTrackChangedEventPayload(subtitleTrack, oldSubtitleTrack)
    }

    data class AudioTrackChanged(val audioTrack: AudioTrack?, val oldAudioTrack: AudioTrack?) : PlayerEvent() {
        override val name = "audioTrackChange"
        override val jsEventPayload = AudioTrackChangedEventPayload(audioTrack, oldAudioTrack)
    }

    data class VideoTrackChanged(val videoTrack: VideoTrack?, val oldVideoTrack: VideoTrack?) : PlayerEvent() {
        override val name = "videoTrackChange"
        override val jsEventPayload = VideoTrackChangedEventPayload(videoTrack, oldVideoTrack)
    }

    class RenderedFirstFrame : PlayerEvent() {
        override val name = "renderFirstFrame"
        override val emitToJS = false
    }

    data class AvailableSubtitleTracksChanged(
        val availableSubtitleTracks: List<SubtitleTrack>,
        val oldAvailableSubtitleTracks: List<SubtitleTrack>
    ) : PlayerEvent() {
        override val name = "availableSubtitleTracksChange"
        override val jsEventPayload = AvailableSubtitleTracksChangedEventPayload(availableSubtitleTracks, oldAvailableSubtitleTracks)
    }

    data class AvailableAudioTracksChanged(
        val availableAudioTracks: List<AudioTrack>,
        val oldAvailableAudioTracks: List<AudioTrack>
    ) : PlayerEvent() {
        override val name = "availableAudioTracksChange"
        override val jsEventPayload = AvailableAudioTracksChangedEventPayload(availableAudioTracks, oldAvailableAudioTracks)
    }

    data class VideoSourceLoaded(
        val videoSource: VideoSource?,
        val duration: Double,
        val availableVideoTracks: List<VideoTrack>,
        val availableSubtitleTracks: List<SubtitleTrack>,
        val availableAudioTracks: List<AudioTrack>
    ) : PlayerEvent() {
        override val name = "sourceLoad"
        override val jsEventPayload = VideoSourceLoadedEventPayload(
            videoSource,
            duration,
            availableVideoTracks,
            availableSubtitleTracks,
            availableAudioTracks
        )
    }

    data class TimeUpdated(val timeUpdate: TimeUpdate) : PlayerEvent() {
        override val name = "timeUpdate"
        override val jsEventPayload = timeUpdate
    }

    data class AudioMixingModeChanged(val audioMixingMode: AudioMixingMode, val oldAudioMixingMode: AudioMixingMode?) : PlayerEvent() {
        override val name = "audioMixingModeChange"
        override val emitToJS = false
    }

    class PlayedToEnd : PlayerEvent() {
        override val name = "playToEnd"
    }

    data class TargetViewChanged(val newTargetView: HybridNitroVideoView?, val oldTargetView: HybridNitroVideoView?) : PlayerEvent() {
        override val name = "targetViewChange"
        override val emitToJS = false
    }

    fun emit(player: HybridVideoPlayer, listeners: List<VideoPlayerListener>) {
        when (this) {
            is StatusChanged -> listeners.forEach { it.onStatusChanged(player, status, oldStatus, error) }
            is IsPlayingChanged -> listeners.forEach { it.onIsPlayingChanged(player, isPlaying, oldIsPlaying) }
            is VolumeChanged -> listeners.forEach { it.onVolumeChanged(player, volume, oldVolume) }
            is SourceChanged -> listeners.forEach { it.onSourceChanged(player, source, oldSource) }
            is PlaybackRateChanged -> listeners.forEach { it.onPlaybackRateChanged(player, rate, oldRate) }
            is TracksChanged -> listeners.forEach { it.onTracksChanged(player, tracks) }
            is TrackSelectionParametersChanged -> listeners.forEach { it.onTrackSelectionParametersChanged(player, trackSelectionParameters) }
            is TimeUpdated -> listeners.forEach { it.onTimeUpdate(player, timeUpdate) }
            is PlayedToEnd -> listeners.forEach { it.onPlayedToEnd(player) }
            is MutedChanged -> listeners.forEach { it.onMutedChanged(player, muted, oldMuted) }
            is AudioMixingModeChanged -> listeners.forEach { it.onAudioMixingModeChanged(player, audioMixingMode, oldAudioMixingMode) }
            is VideoTrackChanged -> listeners.forEach { it.onVideoTrackChanged(player, videoTrack, oldVideoTrack) }
            is RenderedFirstFrame -> listeners.forEach { it.onRenderedFirstFrame(player) }
            is VideoSourceLoaded -> listeners.forEach { it.onVideoSourceLoaded(player, videoSource, duration, availableVideoTracks, availableSubtitleTracks, availableAudioTracks) }
            is TargetViewChanged -> listeners.forEach { it.onTargetViewChanged(player, newTargetView, oldTargetView) }
            else -> Unit
        }
    }
}
