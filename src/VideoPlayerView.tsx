import React, { forwardRef, useEffect, useImperativeHandle } from 'react'
import type { StyleProp, ViewStyle } from 'react-native'
import { useVideoPlayer, useEventListener } from './hooks/useVideoPlayer'
import { VideoView } from './VideoView'
import type { VideoContentFit } from './types/VideoView.types'
import type { VideoPlayer } from './VideoPlayer'

export interface VideoPlayerViewRef {
  seek: (seconds: number) => void
  pause: () => void
  play: () => void
  resume: () => void
  player: VideoPlayer | null
}

export interface VideoPlayerViewProps {
  source: string | any
  style?: StyleProp<ViewStyle>
  resizeMode?: 'contain' | 'cover' | 'fill'
  contentFit?: 'contain' | 'cover' | 'fill'
  repeat?: boolean
  loop?: boolean
  paused?: boolean
  muted?: boolean
  nativeControls?: boolean
  onLoad?: (duration?: number) => void
  onProgress?: (currentTime: number, duration: number) => void
  onEnd?: () => void
  onError?: (error: any) => void
  testID?: string
}

export const VideoPlayerView = forwardRef<VideoPlayerViewRef, VideoPlayerViewProps>(
  (
    {
      source,
      style,
      resizeMode = 'cover',
      contentFit,
      repeat = false,
      loop,
      paused = false,
      muted = false,
      nativeControls = false,
      onLoad,
      onProgress,
      onEnd,
      onError,
      testID,
    },
    ref
  ) => {
    const isLooping = loop ?? repeat
    const fit: VideoContentFit = (contentFit ?? resizeMode) as VideoContentFit

    const player = useVideoPlayer(source, (p) => {
      p.loop = isLooping
      p.muted = muted
      if (!paused) {
        p.play()
      }
    })

    useEffect(() => {
      if (paused) {
        player.pause()
      } else {
        player.play()
      }
    }, [player, paused])

    useEffect(() => {
      player.muted = muted
    }, [player, muted])

    useEffect(() => {
      player.loop = isLooping
    }, [player, isLooping])

    useEventListener(player, 'sourceLoad', (event) => {
      onLoad?.(event?.duration ?? player.duration)
    })

    useEventListener(player, 'timeUpdate', (event) => {
      onProgress?.(event?.currentTime ?? player.currentTime, player.duration)
    })

    useEventListener(player, 'playToEnd', () => {
      onEnd?.()
    })

    useEventListener(player, 'statusChange', (event) => {
      if (event?.status === 'error') {
        onError?.(event?.error)
      }
    })

    useImperativeHandle(
      ref,
      () => ({
        seek: (seconds: number) => {
          player.currentTime = seconds
        },
        pause: () => {
          player.pause()
        },
        play: () => {
          player.play()
        },
        resume: () => {
          player.play()
        },
        player,
      }),
      [player]
    )

    return (
      <VideoView
        player={player}
        style={style}
        contentFit={fit}
        nativeControls={nativeControls}
        testID={testID}
      />
    )
  }
)

VideoPlayerView.displayName = 'VideoPlayerView'
