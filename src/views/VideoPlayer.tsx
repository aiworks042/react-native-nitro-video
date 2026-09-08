import React, { useMemo } from 'react'
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

export { NativeVideoView } from './NativeVideoView'

export type VideoPlayerRef = HybridRef<
  NitroVideoViewProps,
  NitroVideoViewMethods
>

export interface VideoPlayerProps
  extends Omit<
    NitroVideoViewProps,
    'onLoad' | 'onProgress' | 'onEnd' | 'onError'
  > {
  style?: StyleProp<ViewStyle>
  testID?: string
  resizeMode?: ResizeMode
  onLoad?: (duration: number) => void
  onProgress?: (currentTime: number, duration: number) => void
  onEnd?: () => void
  onError?: (error: string) => void
  hybridRef?: (ref: VideoPlayerRef) => void
}

/**
 * Production-ready Nitro Video Player component for Kandid.
 * Automatically wraps event handlers with `callback(...)` to ensure
 * zero-copy C++ Fabric bridge execution without boolean conversion loss.
 */
export const VideoPlayerView = React.memo(function VideoPlayerView({
  onLoad,
  onProgress,
  onEnd,
  onError,
  hybridRef,
  ...restProps
}: VideoPlayerProps) {
  const wrappedOnLoad = useMemo(
    () => (onLoad ? callback(onLoad) : undefined),
    [onLoad]
  )
  const wrappedOnProgress = useMemo(
    () => (onProgress ? callback(onProgress) : undefined),
    [onProgress]
  )
  const wrappedOnEnd = useMemo(
    () => (onEnd ? callback(onEnd) : undefined),
    [onEnd]
  )
  const wrappedOnError = useMemo(
    () => (onError ? callback(onError) : undefined),
    [onError]
  )
  const wrappedHybridRef = useMemo(
    () => (hybridRef ? callback(hybridRef) : undefined),
    [hybridRef]
  )

  return (
    <NativeVideoView
      {...restProps}
      onLoad={wrappedOnLoad}
      onProgress={wrappedOnProgress}
      onEnd={wrappedOnEnd}
      onError={wrappedOnError}
      hybridRef={wrappedHybridRef}
    />
  )
})

export const VideoPlayer = VideoPlayerView
