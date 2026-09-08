package com.margelo.nitro.nitrovideo.records

import com.margelo.nitro.nitrovideo.enums.PlayerStatus
import java.io.Serializable

interface VideoEventPayload : Serializable

class StatusChangedEventPayload(
    val status: PlayerStatus,
    val oldStatus: PlayerStatus?,
    val error: PlaybackError?
) : VideoEventPayload

class IsPlayingEventPayload(
    val isPlaying: Boolean,
    val oldIsPlaying: Boolean?
) : VideoEventPayload

class VolumeChangedEventPayload(
    val volume: Float,
    val oldVolume: Float?
) : VideoEventPayload

class MutedChangedEventPayload(
    val muted: Boolean,
    val oldMuted: Boolean?
) : VideoEventPayload

class SourceChangedEventPayload(
    val source: VideoSource?,
    val oldSource: VideoSource?
) : VideoEventPayload

class PlaybackRateChangedEventPayload(
    val playbackRate: Float,
    val oldPlaybackRate: Float?
) : VideoEventPayload

class TimeUpdate(
    var currentTime: Double = 0.0,
    var currentOffsetFromLive: Float? = null,
    var currentLiveTimestamp: Long? = null,
    var bufferedPosition: Double = 0.0
) : VideoEventPayload

class SubtitleTrackChangedEventPayload(
    val subtitleTrack: SubtitleTrack?,
    val oldSubtitleTrack: SubtitleTrack?
) : VideoEventPayload

class AudioTrackChangedEventPayload(
    val audioTrack: AudioTrack?,
    val oldAudioTrack: AudioTrack?
) : VideoEventPayload

class VideoTrackChangedEventPayload(
    val videoTrack: VideoTrack?,
    val oldVideoTrack: VideoTrack?
) : VideoEventPayload

class AvailableSubtitleTracksChangedEventPayload(
    val availableSubtitleTracks: List<SubtitleTrack>,
    val oldAvailableSubtitleTracks: List<SubtitleTrack>
) : VideoEventPayload

class AvailableAudioTracksChangedEventPayload(
    val availableAudioTracks: List<AudioTrack>,
    val oldAvailableAudioTracks: List<AudioTrack>
) : VideoEventPayload

class VideoSourceLoadedEventPayload(
    val videoSource: VideoSource?,
    val duration: Double,
    val availableVideoTracks: List<VideoTrack>,
    val availableSubtitleTracks: List<SubtitleTrack>,
    val availableAudioTracks: List<AudioTrack>
) : VideoEventPayload
