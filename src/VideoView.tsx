import React, { createRef, type ReactNode } from 'react'
import { findNodeHandle, requireNativeComponent, UIManager } from 'react-native'
import type { StyleProp, ViewStyle } from 'react-native'
import type { VideoViewProps } from './types/VideoView.types'

// ─────────────────────────────────────────────────────────────────────────────
// isPictureInPictureSupported
// ─────────────────────────────────────────────────────────────────────────────

import { isPictureInPictureSupported as _isPip } from './VideoModule'

/**
 * Returns whether the current device supports Picture in Picture (PiP) mode.
 * @platform android @platform ios
 */
export function isPictureInPictureSupported(): boolean {
  return _isPip()
}

// ─────────────────────────────────────────────────────────────────────────────
// NativeNitroVideoView
// ─────────────────────────────────────────────────────────────────────────────

const NativeNitroVideoView = requireNativeComponent<{
  playerId: number | null
  nativeControls: boolean
  contentFit: string
  allowsPictureInPicture: boolean
  startsPictureInPictureAutomatically: boolean
  requiresLinearPlayback: boolean
  useExoShutter: boolean
  controllerAutoShow: boolean
  onPictureInPictureStart?: () => void
  onPictureInPictureStop?: () => void
  onFullscreenEnter?: () => void
  onFullscreenExit?: () => void
  onFirstFrameRender?: () => void
  style?: StyleProp<ViewStyle>
}>('NitroVideoView')

// ─────────────────────────────────────────────────────────────────────────────
// VideoView — drop-in replacement for expo-video's VideoView
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A component that renders a video.
 *
 * Drop-in replacement for expo-video's `VideoView` — powered by Nitro Modules.
 *
 * @example
 * ```tsx
 * const player = useVideoPlayer('https://example.com/video.mp4')
 *
 * <VideoView
 *   player={player}
 *   style={{ width: '100%', height: 250 }}
 *   contentFit="contain"
 *   nativeControls
 * />
 * ```
 */
export class VideoView extends React.PureComponent<VideoViewProps> {
  /**
   * A ref to the underlying native Nitro view.
   * On Android / iOS this is the Nitro-generated native view proxy.
   */
  nativeRef = createRef<any>()

  /** Enters fullscreen mode. */
  async enterFullscreen(): Promise<void> {
    if (typeof this.nativeRef.current?.enterFullscreen === 'function') {
      return this.nativeRef.current.enterFullscreen()
    }
    const node = findNodeHandle(this.nativeRef.current)
    if (node) {
      UIManager.dispatchViewManagerCommand(node, 'enterFullscreen', [])
    }
  }

  /**
   * Exits fullscreen mode.
   *
   * > **Note:** On Android the JS runtime is paused in fullscreen.
   * > Call this from a native event listener for reliable cross-platform behavior.
   */
  async exitFullscreen(): Promise<void> {
    if (typeof this.nativeRef.current?.exitFullscreen === 'function') {
      return this.nativeRef.current.exitFullscreen()
    }
    const node = findNodeHandle(this.nativeRef.current)
    if (node) {
      UIManager.dispatchViewManagerCommand(node, 'exitFullscreen', [])
    }
  }

  /**
   * Enters Picture in Picture (PiP) mode.
   * Throws if the device does not support PiP.
   * @platform android @platform ios
   */
  async startPictureInPicture(): Promise<void> {
    if (typeof this.nativeRef.current?.startPictureInPicture === 'function') {
      return this.nativeRef.current.startPictureInPicture()
    }
    const node = findNodeHandle(this.nativeRef.current)
    if (node) {
      UIManager.dispatchViewManagerCommand(node, 'startPictureInPicture', [])
    }
  }

  /**
   * Exits Picture in Picture (PiP) mode.
   * @platform android @platform ios
   */
  async stopPictureInPicture(): Promise<void> {
    if (typeof this.nativeRef.current?.stopPictureInPicture === 'function') {
      return this.nativeRef.current.stopPictureInPicture()
    }
    const node = findNodeHandle(this.nativeRef.current)
    if (node) {
      UIManager.dispatchViewManagerCommand(node, 'stopPictureInPicture', [])
    }
  }

  render(): ReactNode {
    const {
      player,
      contentFit = 'contain',
      nativeControls = true,
      allowsPictureInPicture = false,
      startsPictureInPictureAutomatically = false,
      requiresLinearPlayback = false,
      useExoShutter = false,
      controllerAutoShow = true,
      onPictureInPictureStart,
      onPictureInPictureStop,
      onFullscreenEnter,
      onFullscreenExit,
      onFirstFrameRender,
      // These props are valid on VideoViewProps but not forwarded directly
      // to the native layer (they are read-only or web-only):
      fullscreenOptions: _fullscreenOptions,
      buttonOptions: _buttonOptions,
      surfaceType: _surfaceType,
      contentPosition: _contentPosition,
      showsTimecodes: _showsTimecodes,
      allowsVideoFrameAnalysis: _allowsVideoFrameAnalysis,
      playsInline: _playsInline,
      crossOrigin: _crossOrigin,
      useAudioNodePlayback: _useAudioNodePlayback,
      ...viewProps
    } = this.props

    const playerId = player ? _resolvePlayerId(player) : null

    return (
      <NativeNitroVideoView
        ref={this.nativeRef}
        {...viewProps}
        playerId={playerId}
        contentFit={contentFit}
        nativeControls={nativeControls}
        allowsPictureInPicture={allowsPictureInPicture}
        startsPictureInPictureAutomatically={startsPictureInPictureAutomatically}
        requiresLinearPlayback={requiresLinearPlayback}
        useExoShutter={useExoShutter}
        controllerAutoShow={controllerAutoShow}
        onPictureInPictureStart={onPictureInPictureStart}
        onPictureInPictureStop={onPictureInPictureStop}
        onFullscreenEnter={onFullscreenEnter}
        onFullscreenExit={onFullscreenExit}
        onFirstFrameRender={onFirstFrameRender}
      />
    )
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function _resolvePlayerId(player: any): number | null {
  if (typeof player === 'number') return player
  // VideoPlayer concrete instance — access via __nitro_player_id__
  return typeof player?.__nitro_player_id__ === 'number' ? player.__nitro_player_id__ : null
}
