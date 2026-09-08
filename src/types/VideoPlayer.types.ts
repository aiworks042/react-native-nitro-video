import type { VideoPlayerEvents } from './VideoPlayerEvents.types'

// ─────────────────────────────────────────────────────────────────────────────
// VideoPlayer class interface  (mirrors expo-video's VideoPlayer exactly)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A class that represents an instance of the video player.
 * Drop-in replacement for expo-video's VideoPlayer — powered by Nitro Modules.
 */
export declare class VideoPlayer {
  /** Unique internal id used to connect VideoPlayer to VideoView via the Nitro bridge. */
  readonly __nitro_player_id__: number

  // ─── Readonly state ────────────────────────────────────────────────────────

  /** Whether the player is currently playing. */
  readonly playing: boolean

  /**
   * The exact timestamp when the currently displayed video frame was sent from the server,
   * based on the `EXT-X-PROGRAM-DATE-TIME` tag in the livestream metadata.
   * @platform android @platform ios
   */
  readonly currentLiveTimestamp: number | null

  /**
   * Float value indicating the latency of the live stream in seconds.
   * @platform android @platform ios
   */
  readonly currentOffsetFromLive: number | null

  /** Float value indicating the duration of the current video in seconds. */
  readonly duration: number

  /** Boolean value indicating whether the player is currently playing a live stream. */
  readonly isLive: boolean

  /** Indicates the current status of the player. */
  readonly status: VideoPlayerStatus

  /**
   * Float value indicating how far the player has buffered the video in seconds.
   * -1 when impossible to determine.
   */
  readonly bufferedPosition: number

  /** Array of subtitle tracks available for the current video. */
  readonly availableSubtitleTracks: SubtitleTrack[]

  /** Array of audio tracks available for the current video. */
  readonly availableAudioTracks: AudioTrack[]

  /** Array of video tracks available for the current video. */
  readonly availableVideoTracks: VideoTrack[]

  /** Current video track being played. null when no video is displayed. */
  readonly videoTrack: VideoTrack | null

  /**
   * Indicates whether the player is playing to an external device via AirPlay.
   * @platform ios
   */
  readonly isExternalPlaybackActive: boolean

  // ─── Read-write properties ─────────────────────────────────────────────────

  /** @default false */
  loop: boolean

  /** @default true  @platform ios */
  allowsExternalPlayback: boolean

  /** @default 'auto' */
  audioMixingMode: AudioMixingMode

  /** @default false */
  muted: boolean

  /** Current playback position in seconds. Setting this seeks the player. */
  currentTime: number

  /** @platform ios */
  targetOffsetFromLive: number

  /** Float 0–1. @default 1.0 */
  volume: number

  /** @default true */
  preservesPitch: boolean

  /** Interval in seconds for timeUpdate. 0 = disabled. @default 0 */
  timeUpdateEventInterval: number

  /** Float 0–16. @default 1.0 */
  playbackRate: number

  /** @default true */
  keepScreenOnWhilePlaying: boolean

  /** @default false */
  showNowPlayingNotification: boolean

  /** @default false */
  staysActiveInBackground: boolean

  bufferOptions: BufferOptions

  /** null = no subtitles. @default null */
  subtitleTrack: SubtitleTrack | null

  /** null = no audio track selected. @default null */
  audioTrack: AudioTrack | null

  /** Max resolution for adaptive streams. null = unlimited. @default null */
  maxResolution: VideoSize | null

  seekTolerance: SeekTolerance

  scrubbingModeOptions: ScrubbingModeOptions

  // ─── Constructor ───────────────────────────────────────────────────────────

  constructor(
    source: VideoSource,
    useSynchronousReplace?: boolean,
    playerBuilderOptions?: PlayerBuilderOptions
  )

  // ─── Playback control ──────────────────────────────────────────────────────

  play(): void
  pause(): void

  /**
   * Replaces the current source synchronously (can block UI thread on iOS).
   * @deprecated Use replaceAsync instead.
   */
  replace(source: VideoSource, disableWarning?: boolean): void

  /** Replaces the current source asynchronously. */
  replaceAsync(source: VideoSource): Promise<void>

  /** Seeks by the given number of seconds relative to the current position. */
  seekBy(seconds: number): void

  /** Seeks to the beginning and plays. */
  replay(): void

  /**
   * Generates thumbnails at the given time positions.
   * @platform android @platform ios
   */
  generateThumbnailsAsync(
    times: number | number[],
    options?: VideoThumbnailOptions
  ): Promise<VideoThumbnail[]>

  // ─── Event emitter (mirrors expo-video SharedObject pattern) ──────────────

  addListener<K extends keyof VideoPlayerEvents>(
    event: K,
    listener: VideoPlayerEvents[K]
  ): { remove: () => void }

  removeAllListeners<K extends keyof VideoPlayerEvents>(event?: K): void
}

// ─────────────────────────────────────────────────────────────────────────────
// Scalar / union types
// ─────────────────────────────────────────────────────────────────────────────

/**
 * - `idle`: Not playing or loading.
 * - `loading`: Loading video data.
 * - `readyToPlay`: Enough data to play.
 * - `error`: An error occurred.
 */
export type VideoPlayerStatus = 'idle' | 'loading' | 'readyToPlay' | 'error'

export type VideoSource = string | number | null | VideoSourceObject

export type VideoSourceObject = {
  uri?: string
  assetId?: number
  drm?: DRMOptions
  metadata?: VideoMetadata
  headers?: Record<string, string>
  /** @default false */
  useCaching?: boolean
  /** @default 'auto' */
  contentType?: ContentType
}

export type PlayerError = { message: string }

export type VideoMetadata = {
  title?: string
  artist?: string
  artwork?: string
}

export type DRMType = 'clearkey' | 'fairplay' | 'playready' | 'widevine'

export type DRMOptions = {
  type: DRMType
  licenseServer: string
  headers?: Record<string, string>
  /** @platform android */
  multiKey?: boolean
  /** @platform ios */
  contentId?: string
  /** @platform ios */
  certificateUrl?: string
  /** @platform ios */
  base64CertificateData?: string
}

/**
 * Buffer options for the player.
 * @platform android @platform ios
 */
export type BufferOptions = {
  /** @default android: 20, ios: 0 */
  readonly preferredForwardBufferDuration?: number
  /** @default true @platform ios */
  readonly waitsToMinimizeStalling?: boolean
  /** @default 2 @platform android */
  readonly minBufferForPlayback?: number
  /** @default 0 @platform android */
  readonly maxBufferBytes?: number | null
  /** @default false @platform android */
  readonly prioritizeTimeOverSizeThreshold?: boolean
}

/**
 * - `auto`: Player auto-detects content type.
 * - `progressive`: Progressive download.
 * - `hls`: HTTP Live Streaming.
 * - `dash`: MPEG-DASH (Android only).
 * - `smoothStreaming`: Microsoft SmoothStreaming (Android only).
 */
export type ContentType = 'auto' | 'progressive' | 'hls' | 'dash' | 'smoothStreaming'

/**
 * - `mixWithOthers`: Mix with other apps.
 * - `duckOthers`: Lower volume of other apps.
 * - `auto`: Interrupt only when needed.
 * - `doNotMix`: Pause other apps.
 */
export type AudioMixingMode = 'mixWithOthers' | 'duckOthers' | 'auto' | 'doNotMix'

export type SubtitleTrack = {
  /** @platform android */
  id?: string
  language: string
  label: string
  name?: string
  isDefault?: boolean
  autoSelect?: boolean
}

export type VideoTrack = {
  id: string
  url: string | null
  size: VideoSize
  mimeType: string | null
  /** @platform android */
  isSupported: boolean
  /** @deprecated Use peakBitrate or averageBitrate */
  bitrate: number | null
  averageBitrate: number | null
  peakBitrate: number | null
  frameRate: number | null
  videoRange: VideoRange
}

export type VideoSize = {
  width: number
  height: number
}

export type AudioTrack = {
  /** @platform android */
  id?: string
  language: string
  label: string
  name?: string
  isDefault?: boolean
  autoSelect?: boolean
}

export type SeekTolerance = {
  /** @default 0 */
  toleranceBefore?: number
  /** @default 0 */
  toleranceAfter?: number
}

export type ScrubbingModeOptions = {
  /** @default false */
  scrubbingModeEnabled?: boolean
  /** @default true @platform android */
  increaseCodecOperatingRate?: boolean
  /** @default true @platform android */
  enableDynamicScheduling?: boolean
  /** @default true @platform android */
  useDecodeOnlyFlag?: boolean
  /** @default true @platform android */
  allowSkippingMediaCodecFlush?: boolean
}

/** @platform android */
export type VideoChangeFrameRateStrategy = 'off' | 'onlyIfSeamless'

/** @platform android */
export type PlayerBuilderOptions = {
  seekBackwardIncrement?: number
  seekForwardIncrement?: number
  /** @default 'onlyIfSeamless' */
  videoChangeFrameRateStrategy?: VideoChangeFrameRateStrategy
}

/**
 * - `sdr`: Standard Dynamic Range.
 * - `hlg`: Hybrid Log-Gamma.
 * - `pq`: Perceptual Quantizer (HDR10, Dolby Vision).
 */
export type VideoRange = 'sdr' | 'hlg' | 'pq'

export type VideoThumbnailOptions = {
  maxWidth?: number
  maxHeight?: number
}

// ─────────────────────────────────────────────────────────────────────────────
// VideoThumbnail — native image reference
// ─────────────────────────────────────────────────────────────────────────────

export declare class VideoThumbnail {
  readonly width: number
  readonly height: number
  readonly requestedTime: number
  /** @platform ios */
  readonly actualTime: number
}
