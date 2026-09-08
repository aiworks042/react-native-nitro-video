import { NitroModules } from 'react-native-nitro-modules'
import type { VideoModule } from './specs/HybridVideoModule.nitro'

// Lazy singleton — only created on first call
let _module: VideoModule | null = null

function getModule(): VideoModule {
  if (!_module) {
    _module = NitroModules.createHybridObject<VideoModule>('VideoModule')
  }
  return _module
}

// ─────────────────────────────────────────────────────────────────────────────
// VideoModule — mirrors expo-video's VideoModule exports exactly
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns whether the current device supports Picture in Picture (PiP) mode.
 * @platform android @platform ios
 */
export function isPictureInPictureSupported(): boolean {
  try {
    return getModule().isPictureInPictureSupported()
  } catch {
    return false
  }
}

/**
 * Clears all video cache.
 *
 * > This function can only be called when there are no existing `VideoPlayer` instances.
 *
 * @platform android @platform ios
 */
export function clearVideoCacheAsync(): Promise<void> {
  return getModule().clearVideoCacheAsync()
}

/**
 * Sets the desired video cache size in bytes.
 * The default video cache size is 1 GB. The value is persistent across restarts.
 *
 * > This function can only be called when there are no existing `VideoPlayer` instances.
 *
 * @platform android @platform ios
 */
export function setVideoCacheSizeAsync(sizeBytes: number): Promise<void> {
  return getModule().setVideoCacheSizeAsync(sizeBytes)
}

/**
 * Returns the space currently occupied by the video cache in bytes.
 *
 * @platform android @platform ios
 */
export function getCurrentVideoCacheSize(): number {
  return getModule().getCurrentVideoCacheSize()
}
