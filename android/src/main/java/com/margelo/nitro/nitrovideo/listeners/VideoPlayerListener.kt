package com.margelo.nitro.nitrovideo.listeners

import androidx.annotation.OptIn
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.HybridNitroVideoView
import com.margelo.nitro.nitrovideo.HybridVideoPlayer
import com.margelo.nitro.nitrovideo.enums.AudioMixingMode
import com.margelo.nitro.nitrovideo.enums.PlayerStatus
import com.margelo.nitro.nitrovideo.records.AudioTrack
import com.margelo.nitro.nitrovideo.records.PlaybackError
import com.margelo.nitro.nitrovideo.records.SubtitleTrack
import com.margelo.nitro.nitrovideo.records.TimeUpdate
import com.margelo.nitro.nitrovideo.records.VideoSource
import com.margelo.nitro.nitrovideo.records.VideoTrack

@OptIn(UnstableApi::class)
interface VideoPlayerListener {
    fun onStatusChanged(player: HybridVideoPlayer, status: PlayerStatus, oldStatus: PlayerStatus?, error: PlaybackError?) {}
    fun onIsPlayingChanged(player: HybridVideoPlayer, isPlaying: Boolean, oldIsPlaying: Boolean?) {}
    fun onVolumeChanged(player: HybridVideoPlayer, volume: Float, oldVolume: Float?) {}
    fun onMutedChanged(player: HybridVideoPlayer, muted: Boolean, oldMuted: Boolean?) {}
    fun onSourceChanged(player: HybridVideoPlayer, source: VideoSource?, oldSource: VideoSource?) {}
    fun onPlaybackRateChanged(player: HybridVideoPlayer, rate: Float, oldRate: Float?) {}
    fun onTracksChanged(player: HybridVideoPlayer, tracks: Tracks) {}
    fun onTrackSelectionParametersChanged(player: HybridVideoPlayer, trackSelectionParameters: TrackSelectionParameters) {}
    fun onTimeUpdate(player: HybridVideoPlayer, timeUpdate: TimeUpdate) {}
    fun onPlayedToEnd(player: HybridVideoPlayer) {}
    fun onAudioMixingModeChanged(player: HybridVideoPlayer, audioMixingMode: AudioMixingMode, oldAudioMixingMode: AudioMixingMode?) {}
    fun onVideoTrackChanged(player: HybridVideoPlayer, videoTrack: VideoTrack?, oldVideoTrack: VideoTrack?) {}
    fun onVideoSourceLoaded(
        player: HybridVideoPlayer,
        videoSource: VideoSource?,
        duration: Double?,
        availableVideoTracks: List<VideoTrack>,
        availableSubtitleTracks: List<SubtitleTrack>,
        availableAudioTracks: List<AudioTrack>
    ) {}
    fun onTargetViewChanged(player: HybridVideoPlayer, newTargetView: HybridNitroVideoView?, oldTargetView: HybridNitroVideoView?) {}
    fun onRenderedFirstFrame(player: HybridVideoPlayer) {}
}
