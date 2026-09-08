// ─────────────────────────────────────────────────────────────────────────────
// react-native-nitro-video
//
// Drop-in replacement for expo-video powered by Nitro Modules.
// All exports are API-compatible with expo-video — swap the import and go.
//
//   import { VideoView, useVideoPlayer } from 'react-native-nitro-video'
//
// ─────────────────────────────────────────────────────────────────────────────

// ─── Core Components ─────────────────────────────────────────────────────────
export { VideoView, isPictureInPictureSupported } from './VideoView'

// ─── Player class & factory ──────────────────────────────────────────────────
export { VideoPlayer, createVideoPlayer } from './VideoPlayer'

// ─── Hooks ───────────────────────────────────────────────────────────────────
export { useVideoPlayer, useEventListener } from './hooks/useVideoPlayer'

// ─── Module-level utilities ───────────────────────────────────────────────────
export {
  clearVideoCacheAsync,
  setVideoCacheSizeAsync,
  getCurrentVideoCacheSize,
} from './VideoModule'

// ─── Additional UI Components & Classes ──────────────────────────────────────
export { VideoAirPlayButton } from './VideoAirPlayButton'
export { VideoThumbnail } from './VideoThumbnail'
export type { VideoAirPlayButtonProps } from './VideoAirPlayButton.types'

// ─── Types (re-export everything so consumers only need one import path) ──────
export type {
  // Player
  VideoPlayerStatus,
  VideoSource,
  VideoSourceObject,
  PlayerError,
  VideoMetadata,
  DRMType,
  DRMOptions,
  BufferOptions,
  ContentType,
  AudioMixingMode,
  SubtitleTrack,
  VideoTrack,
  VideoSize,
  AudioTrack,
  SeekTolerance,
  ScrubbingModeOptions,
  VideoChangeFrameRateStrategy,
  PlayerBuilderOptions,
  VideoRange,
  VideoThumbnailOptions,
} from './types/VideoPlayer.types'

export type {
  // Events
  VideoPlayerEvents,
  StatusChangeEventPayload,
  PlayingChangeEventPayload,
  PlaybackRateChangeEventPayload,
  VolumeChangeEventPayload,
  MutedChangeEventPayload,
  SourceChangeEventPayload,
  TimeUpdateEventPayload,
  SubtitleTrackChangeEventPayload,
  VideoTrackChangeEventPayload,
  AvailableSubtitleTracksChangeEventPayload,
  SourceLoadEventPayload,
  IsExternalPlaybackActiveChangeEventPayload,
} from './types/VideoPlayerEvents.types'

export type {
  // View
  VideoViewProps,
  VideoContentFit,
  SurfaceType,
  FullscreenOrientation,
  KeepFullscreenOnPiPStopBehavior,
  FullscreenOptions,
  ButtonOptions,
} from './types/VideoView.types'
