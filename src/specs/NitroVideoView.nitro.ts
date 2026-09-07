import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules'
import type { ResizeMode } from './ResizeMode'

export interface NitroVideoViewProps extends HybridViewProps {
  source: string
  paused?: boolean
  muted?: boolean
  repeat?: boolean
  volume?: number
  resizeMode?: ResizeMode
  onLoad?: (duration: number) => void
  onProgress?: (currentTime: number, duration: number) => void
  onEnd?: () => void
  onError?: (error: string) => void
}

export interface NitroVideoViewMethods extends HybridViewMethods {
  play(): void
  pause(): void
  seek(position: number): void
}

export type NitroVideoView = HybridView<
  NitroVideoViewProps,
  NitroVideoViewMethods
>
