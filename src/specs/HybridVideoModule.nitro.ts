import type { HybridObject } from 'react-native-nitro-modules'
import type { VideoContentFit } from './HybridVideoPlayer.nitro'

// ─────────────────────────────────────────────────────────────────────────────
// HybridVideoModule — Nitro spec for the top-level VideoModule
// Manages PiP support check, cache management, and player factory.
// ─────────────────────────────────────────────────────────────────────────────

export interface VideoModule
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  /** Returns true if the device supports Picture-in-Picture mode. */
  isPictureInPictureSupported(): boolean

  /** Clears all video cache. May only be called when no VideoPlayer instances exist. */
  clearVideoCacheAsync(): Promise<void>

  /**
   * Sets the desired video cache size in bytes. Default: 1 GB.
   * May only be called when no VideoPlayer instances exist.
   */
  setVideoCacheSizeAsync(sizeBytes: number): Promise<void>

  /** Returns the current video cache size in bytes. */
  getCurrentVideoCacheSize(): number
}

// ─────────────────────────────────────────────────────────────────────────────
// NitroVideoView — Nitro spec for the VideoView native view
// ─────────────────────────────────────────────────────────────────────────────

export interface NitroVideoView
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  // ─── Props set from JS ──────────────────────────────────────────────────

  /** Nitro player id — connects VideoView to a VideoPlayer instance. */
  playerId?: number

  nativeControls: boolean
  contentFit: VideoContentFit
  allowsPictureInPicture: boolean
  startsPictureInPictureAutomatically: boolean
  requiresLinearPlayback: boolean
  useExoShutter: boolean
  controllerAutoShow: boolean

  // ─── Imperative methods ─────────────────────────────────────────────────

  enterFullscreen(): Promise<void>
  exitFullscreen(): Promise<void>
  startPictureInPicture(): Promise<void>
  stopPictureInPicture(): Promise<void>

  // ─── Callbacks ──────────────────────────────────────────────────────────

  onPictureInPictureStart?: () => void
  onPictureInPictureStop?: () => void
  onFullscreenEnter?: () => void
  onFullscreenExit?: () => void
  onFirstFrameRender?: () => void
}
