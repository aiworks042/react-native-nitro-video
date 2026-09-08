import type {
  PlayerError,
  SubtitleTrack,
  VideoPlayerStatus,
  VideoSource,
  VideoTrack,
  AudioTrack,
} from './VideoPlayer.types'

// ─────────────────────────────────────────────────────────────────────────────
// VideoPlayerEvents — identical to expo-video's type, zero Expo imports
// ─────────────────────────────────────────────────────────────────────────────

/** All events that a VideoPlayer instance can emit. */
export type VideoPlayerEvents = {
  /** Emitted when the status of the player changes. */
  statusChange(payload: StatusChangeEventPayload): void

  /** Emitted when the player starts or stops playback. */
  playingChange(payload: PlayingChangeEventPayload): void

  /** Emitted when `playbackRate` changes. */
  playbackRateChange(payload: PlaybackRateChangeEventPayload): void

  /** Emitted when `volume` or `muted` changes. */
  volumeChange(payload: VolumeChangeEventPayload): void

  /** Emitted when `muted` changes. */
  mutedChange(payload: MutedChangeEventPayload): void

  /** Emitted when the player plays to the end of the current source. */
  playToEnd(): void

  /** Emitted at the interval specified by `timeUpdateEventInterval`. */
  timeUpdate(payload: TimeUpdateEventPayload): void

  /** Emitted when the current media source changes. */
  sourceChange(payload: SourceChangeEventPayload): void

  /** Emitted when the available subtitle tracks change. */
  availableSubtitleTracksChange(
    payload: AvailableSubtitleTracksChangeEventPayload
  ): void

  /** Emitted when the current subtitle track changes. */
  subtitleTrackChange(payload: SubtitleTrackChangeEventPayload): void

  /** Emitted when the available audio tracks change. */
  availableAudioTracksChange(
    payload: AvailableAudioTracksChangeEventPayload
  ): void

  /** Emitted when the current audio track changes. */
  audioTrackChange(payload: AudioTrackChangeEventPayload): void

  /** Emitted when the current video track changes. */
  videoTrackChange(payload: VideoTrackChangeEventPayload): void

  /**
   * Emitted when the player has finished loading metadata for the current source.
   * Does not mean there is enough data buffered to start playback.
   */
  sourceLoad(payload: SourceLoadEventPayload): void

  /**
   * Emitted when AirPlay starts or stops.
   * @platform ios
   */
  isExternalPlaybackActiveChange(
    payload: IsExternalPlaybackActiveChangeEventPayload
  ): void
}

// ─────────────────────────────────────────────────────────────────────────────
// Event payload types
// ─────────────────────────────────────────────────────────────────────────────

export type StatusChangeEventPayload = {
  status: VideoPlayerStatus
  oldStatus?: VideoPlayerStatus
  error?: PlayerError
}

export type PlayingChangeEventPayload = {
  isPlaying: boolean
  oldIsPlaying?: boolean
}

export type PlaybackRateChangeEventPayload = {
  playbackRate: number
  oldPlaybackRate?: number
}

export type VolumeChangeEventPayload = {
  volume: number
  oldVolume?: number
}

export type MutedChangeEventPayload = {
  muted: boolean
  oldMuted?: boolean
}

export type SourceChangeEventPayload = {
  source: VideoSource
  oldSource?: VideoSource
}

export type TimeUpdateEventPayload = {
  /** Same as the `currentTime` property. */
  currentTime: number
  /** @platform android @platform ios */
  currentLiveTimestamp: number | null
  /** @platform android @platform ios */
  currentOffsetFromLive: number | null
  /** @platform android @platform ios */
  bufferedPosition: number
}

export type SubtitleTrackChangeEventPayload = {
  subtitleTrack: SubtitleTrack | null
  oldSubtitleTrack?: SubtitleTrack | null
}

export type VideoTrackChangeEventPayload = {
  videoTrack: VideoTrack | null
  oldVideoTrack?: VideoTrack | null
}

export type AvailableSubtitleTracksChangeEventPayload = {
  availableSubtitleTracks: SubtitleTrack[]
  oldAvailableSubtitleTracks?: SubtitleTrack[]
}

export type SourceLoadEventPayload = {
  videoSource: VideoSource | null
  duration: number
  availableVideoTracks: VideoTrack[]
  availableSubtitleTracks: SubtitleTrack[]
  availableAudioTracks: AudioTrack[]
}

type AudioTrackChangeEventPayload = {
  audioTrack: AudioTrack | null
  oldAudioTrack?: AudioTrack | null
}

type AvailableAudioTracksChangeEventPayload = {
  availableAudioTracks: AudioTrack[]
  oldAvailableAudioTracks?: AudioTrack[]
}

export type IsExternalPlaybackActiveChangeEventPayload = {
  isExternalPlaybackActive: boolean
  oldIsExternalPlaybackActive?: boolean
}
