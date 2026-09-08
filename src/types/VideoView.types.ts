import type { ViewProps } from 'react-native'
import type { VideoPlayer } from './VideoPlayer.types'

// ─────────────────────────────────────────────────────────────────────────────
// VideoView types — identical to expo-video, zero Expo imports
// ─────────────────────────────────────────────────────────────────────────────

/**
 * - `contain`: Letterbox/pillarbox to fit inside the container.
 * - `cover`: Crop to fill the container.
 * - `fill`: Stretch/squeeze to fill.
 */
export type VideoContentFit = 'contain' | 'cover' | 'fill'

/**
 * Android surface type.
 * - `surfaceView`: Default — lower power, more features.
 * - `textureView`: Use when SurfaceView causes issues (e.g. overlapping views).
 * @platform android
 */
export type SurfaceType = 'textureView' | 'surfaceView'

export type FullscreenOrientation =
  | 'default'
  | 'portrait'
  | 'portraitUp'
  | 'portraitDown'
  | 'landscape'
  | 'landscapeLeft'
  | 'landscapeRight'

export type KeepFullscreenOnPiPStopBehavior = 'always' | 'autoEnter' | 'never'

export type FullscreenOptions = {
  /** @default true */
  enable: boolean
  /** @default 'default' @platform android @platform ios */
  orientation?: FullscreenOrientation
  /** @default false @platform android @platform ios */
  autoExitOnRotate?: boolean
  /** @default 'autoEnter' @platform ios */
  keepFullscreenOnPiPStop?: KeepFullscreenOnPiPStopBehavior
}

/**
 * Controls visibility of player control buttons.
 * @platform android
 */
export type ButtonOptions = {
  /** @default false */
  showNext?: boolean
  /** @default false */
  showPrevious?: boolean
  /** @default true */
  showSeekForward?: boolean
  /** @default true */
  showSeekBackward?: boolean
  /** undefined = visible only when subtitles are available */
  showSubtitles?: boolean | null
  /** @default true */
  showSettings?: boolean
  /** @default true */
  showPlayPause?: boolean
  /**
   * Show bottom control bar (time, progress, buttons).
   * Always visible in fullscreen.
   * @default true
   */
  showBottomBar?: boolean
}

export interface VideoViewProps extends ViewProps {
  /**
   * A video player instance. Create one with `useVideoPlayer()`.
   */
  player?: VideoPlayer | null

  /**
   * Whether native controls should be shown.
   * @default true
   */
  nativeControls?: boolean

  /**
   * How the video should be scaled to fit the container.
   * @default 'contain'
   */
  contentFit?: VideoContentFit

  /** Fullscreen mode options. */
  fullscreenOptions?: FullscreenOptions

  /**
   * Whether timecodes should be displayed.
   * @default true @platform ios
   */
  showsTimecodes?: boolean

  /**
   * Prevents the user from skipping media content.
   * @default false @platform android @platform ios
   */
  requiresLinearPlayback?: boolean

  /**
   * Control button visibility config.
   * @platform android
   */
  buttonOptions?: ButtonOptions

  /**
   * Android surface render type. Do NOT change at runtime.
   * @default 'surfaceView' @platform android
   */
  surfaceType?: SurfaceType

  /**
   * Position offset of the video inside the container.
   * @default { dx: 0, dy: 0 } @platform ios
   */
  contentPosition?: { dx?: number; dy?: number }

  /**
   * Called after entering Picture in Picture mode.
   * @platform android @platform ios
   */
  onPictureInPictureStart?: () => void

  /**
   * Called after exiting Picture in Picture mode.
   * @platform android @platform ios
   */
  onPictureInPictureStop?: () => void

  /**
   * Whether PiP mode is allowed.
   * @platform android @platform ios
   */
  allowsPictureInPicture?: boolean

  /**
   * Whether to start PiP automatically when app backgrounds.
   * @default false @platform android 12+ @platform ios
   */
  startsPictureInPictureAutomatically?: boolean

  /**
   * Whether to perform video frame analysis (Live Text).
   * @default true @platform ios 16.0+
   */
  allowsVideoFrameAnalysis?: boolean

  /** Called after entering fullscreen. */
  onFullscreenEnter?: () => void

  /** Called after exiting fullscreen. */
  onFullscreenExit?: () => void

  /**
   * Called when the first video frame is rendered.
   * Useful for hiding cover images.
   */
  onFirstFrameRender?: () => void

  /**
   * Whether to use the default ExoPlayer shutter (covers VideoView before first frame).
   * false = same behavior as iOS.
   * @platform android @default false
   */
  useExoShutter?: boolean

  /**
   * Whether native controls auto-show on play/pause/end.
   * @default true @platform android
   */
  controllerAutoShow?: boolean

  /**
   * Whether the player plays inline (web only).
   * @platform web
   */
  playsInline?: boolean

  /**
   * CORS policy for the underlying video element (web only).
   * @default undefined @platform web
   */
  crossOrigin?: 'anonymous' | 'use-credentials'

  /**
   * Use Audio Nodes for sound (web only, experimental).
   * @default false @platform web
   */
  useAudioNodePlayback?: boolean
}
