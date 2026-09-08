import type { HybridObject } from 'react-native-nitro-modules'

// ─────────────────────────────────────────────────────────────────────────────
// NITROGEN RULES — types used in .nitro.ts specs must follow these rules:
//   ✅ Plain interfaces (no classes)
//   ✅ TypeScript string enums (not union types like 'a' | 'b')
//   ✅ number, boolean, string, void, null
//   ✅ Arrays of the above
//   ✅ Optional properties with ?
//   ❌ declare class
//   ❌ string unions ('a' | 'b') — use enum instead
//   ❌ unknown / any
// ─────────────────────────────────────────────────────────────────────────────

// ─── String Unions (Nitrogen supports "a" | "b" string unions) ─────────────

export type VideoPlayerStatus = 'idle' | 'loading' | 'readyToPlay' | 'error'

export type AudioMixingMode =
  | 'mixWithOthers'
  | 'duckOthers'
  | 'auto'
  | 'doNotMix'

export type VideoContentFit = 'contain' | 'cover' | 'fill'

// ─── Flat interfaces (Nitrogen-safe: no optional unions with null) ────────────

export interface NitroVideoSize {
  width: number
  height: number
}

export interface NitroSubtitleTrack {
  id: string
  language: string
  label: string
  name: string
  isDefault: boolean
  autoSelect: boolean
}

export interface NitroAudioTrack {
  id: string
  language: string
  label: string
  name: string
  isDefault: boolean
  autoSelect: boolean
}

export interface NitroVideoTrack {
  id: string
  url: string
  width: number
  height: number
  mimeType: string
  isSupported: boolean
  averageBitrate: number
  peakBitrate: number
  frameRate: number
  videoRange: string
}

export interface NitroVideoThumbnail {
  width: number
  height: number
  requestedTime: number
  actualTime: number
}

export interface NitroThumbnailOptions {
  maxWidth: number
  maxHeight: number
}

export interface NitroBufferOptions {
  preferredForwardBufferDuration: number
  waitsToMinimizeStalling: boolean
  minBufferForPlayback: number
  maxBufferBytes: number
  prioritizeTimeOverSizeThreshold: boolean
}

export interface NitroSeekTolerance {
  toleranceBefore: number
  toleranceAfter: number
}

export interface NitroScrubbingModeOptions {
  scrubbingModeEnabled: boolean
  increaseCodecOperatingRate: boolean
  enableDynamicScheduling: boolean
  useDecodeOnlyFlag: boolean
  allowSkippingMediaCodecFlush: boolean
}

export interface NitroVideoSource {
  uri: string
  useCaching: boolean
  contentType: string
}

export interface NitroPlayerBuilderOptions {
  seekBackwardIncrement: number
  seekForwardIncrement: number
}

// ─────────────────────────────────────────────────────────────────────────────
// HybridVideoPlayer — the main Nitro spec
// Nitrogen reads this file and generates:
//   - C++ HybridVideoPlayerSpec.hpp
//   - Swift HybridVideoPlayerSpec.swift (protocol)
//   - Kotlin HybridVideoPlayerSpec.kt (abstract class)
// ─────────────────────────────────────────────────────────────────────────────

export interface VideoPlayer
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {

  // ─── Readonly state ────────────────────────────────────────────────────────

  readonly id: number
  readonly playing: boolean
  readonly duration: number
  readonly isLive: boolean
  readonly status: VideoPlayerStatus
  readonly bufferedPosition: number
  readonly isExternalPlaybackActive: boolean

  // ─── Read-write properties ─────────────────────────────────────────────────

  loop: boolean
  allowsExternalPlayback: boolean
  audioMixingMode: AudioMixingMode
  muted: boolean
  currentTime: number
  volume: number
  preservesPitch: boolean
  timeUpdateEventInterval: number
  playbackRate: number
  keepScreenOnWhilePlaying: boolean
  showNowPlayingNotification: boolean
  staysActiveInBackground: boolean

  // ─── Methods ──────────────────────────────────────────────────────────────

  /** Load a source and start buffering. */
  load(uri: string, useCaching: boolean, contentType: string): void

  play(): void
  pause(): void

  /** Async source replacement — does not block UI thread. */
  replaceAsync(uri: string): Promise<void>

  /** Seek relative to current position in seconds. */
  seekBy(seconds: number): void

  /** Seek to beginning and play. */
  replay(): void

  /** Set buffer options. */
  setBufferOptions(options: NitroBufferOptions): void

  /** Set seek tolerance. */
  setSeekTolerance(toleranceBefore: number, toleranceAfter: number): void

  /** Set scrubbing mode options. */
  setScrubbingModeOptions(options: NitroScrubbingModeOptions): void

  /** Set the active subtitle track by id. Empty string = none. */
  setSubtitleTrack(trackId: string): void

  /** Set the active audio track by id. Empty string = none. */
  setAudioTrack(trackId: string): void

  /** Set max resolution cap for adaptive streams. 0 = unlimited. */
  setMaxResolution(width: number, height: number): void

  /** Generate thumbnails at the given time positions (seconds). */
  generateThumbnailsAsync(
    times: number[],
    maxWidth: number,
    maxHeight: number
  ): Promise<NitroVideoThumbnail[]>

  // ─── Event system ──────────────────────────────────────────────────────────

  /** Attach an event listener. Returns a listener id for removal. */
  addListener(event: string, callback: (payload: string) => void): number

  /** Remove a listener by id. */
  removeListener(listenerId: number): void
}
