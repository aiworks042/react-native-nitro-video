// Raw host component
export { NativeVideoView } from './views/NativeVideoView'

// Expo-style API
export { VideoView } from './views/VideoView'
export type { VideoViewProps, VideoViewRef, VideoContentFit } from './views/VideoView'
export {
  useVideoPlayer,
  createVideoPlayer,
  VideoPlayer,
  parseSource,
} from './hooks/useVideoPlayer'
export type {
  VideoSource,
  VideoPlayerInstance,
  VideoPlayerStatus,
  VideoPlayerEvents,
  StatusChangeEventPayload,
  PlayingChangeEventPayload,
  TimeUpdateEventPayload,
  VolumeChangeEventPayload,
  MutedChangeEventPayload,
  EventSubscription,
} from './hooks/useVideoPlayer'

// Classic declarative component API
export {
  VideoPlayerView,
  VideoPlayer as VideoPlayerComponent,
} from './views/VideoPlayer'
export type { VideoPlayerProps, VideoPlayerRef } from './views/VideoPlayer'

// Specs and Types
export type { ResizeMode } from './specs/ResizeMode'
export type {
  NitroVideoView,
  NitroVideoViewProps,
  NitroVideoViewMethods,
} from './specs/NitroVideoView.nitro'
