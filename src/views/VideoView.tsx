import React, {
  useCallback,
  useMemo,
  forwardRef,
  useImperativeHandle,
  useRef,
} from 'react'
import type { StyleProp, ViewStyle } from 'react-native'
import {
  callback,
  type HybridRef,
} from 'react-native-nitro-modules'
import { NativeVideoView } from './NativeVideoView'
import type {
  NitroVideoViewProps,
  NitroVideoViewMethods,
} from '../specs/NitroVideoView.nitro'
import type { ResizeMode } from '../specs/ResizeMode'
import type { VideoPlayer } from '../hooks/useVideoPlayer'

export type VideoContentFit = 'contain' | 'cover' | 'fill'

export interface VideoViewProps {
  player?: VideoPlayer | null
  contentFit?: VideoContentFit
  style?: StyleProp<ViewStyle>
  nativeControls?: boolean
}

export interface VideoViewRef {
  enterFullscreen(): Promise<void>
  exitFullscreen(): Promise<void>
}

/**
 * Modern VideoView component mirroring Expo's `<VideoView />`.
 * Powered under the hood by high-performance Nitro Fabric C++ ShadowNodes.
 *
 * @example
 * ```tsx
 * const player = useVideoPlayer(videoUrl, (p) => p.play());
 * return <VideoView player={player} contentFit="cover" style={styles.video} />;
 * ```
 */
export const VideoView = React.memo(
  forwardRef<VideoViewRef, VideoViewProps>(function VideoView(
    { player, contentFit = 'contain', style }: VideoViewProps,
    ref
  ) {
    const internalNativeRef = useRef<HybridRef<
      NitroVideoViewProps,
      NitroVideoViewMethods
    > | null>(null)

    useImperativeHandle(ref, () => ({
      async enterFullscreen() {
        // Fullscreen hook placeholder
      },
      async exitFullscreen() {
        // Exit fullscreen placeholder
      },
    }))

    const resizeMode: ResizeMode = useMemo(() => {
      switch (contentFit) {
        case 'cover':
          return 'cover'
        case 'fill':
          return 'stretch'
        case 'contain':
        default:
          return 'contain'
      }
    }, [contentFit])

    const onLoad = useCallback(
      (duration: number) => {
        player?._onLoad(duration)
      },
      [player]
    )

    const onProgress = useCallback(
      (currentTime: number, duration: number) => {
        player?._onProgress(currentTime, duration)
      },
      [player]
    )

    const onEnd = useCallback(() => {
      player?._onEnd()
    }, [player])

    const onError = useCallback(
      (err: string) => {
        player?._onError(err)
      },
      [player]
    )

    const hybridRef = useCallback(
      (nativeRef: HybridRef<NitroVideoViewProps, NitroVideoViewMethods>) => {
        internalNativeRef.current = nativeRef
        player?._attachNativeRef(nativeRef)
      },
      [player]
    )

    const wrappedOnLoad = useMemo(() => callback(onLoad), [onLoad])
    const wrappedOnProgress = useMemo(() => callback(onProgress), [onProgress])
    const wrappedOnEnd = useMemo(() => callback(onEnd), [onEnd])
    const wrappedOnError = useMemo(() => callback(onError), [onError])
    const wrappedHybridRef = useMemo(() => callback(hybridRef), [hybridRef])

    if (!player) {
      return null
    }

    return (
      <NativeVideoView
        style={style}
        source={player.source}
        paused={!player.playing}
        muted={player.muted}
        repeat={player.loop}
        volume={player.volume}
        resizeMode={resizeMode}
        onLoad={wrappedOnLoad}
        onProgress={wrappedOnProgress}
        onEnd={wrappedOnEnd}
        onError={wrappedOnError}
        hybridRef={wrappedHybridRef}
      />
    )
  })
)
