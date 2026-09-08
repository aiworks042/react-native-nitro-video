import { Image, Platform } from 'react-native'
import type { VideoSource, VideoSourceObject } from '../types/VideoPlayer.types'

// ─────────────────────────────────────────────────────────────────────────────
// resolveVideoSource
//
// Mirrors expo-video's parseSource() — converts the flexible VideoSource
// union type into a concrete VideoSourceObject that the native layer can use.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves a `VideoSource` (string | number | null | VideoSourceObject)
 * into a `VideoSourceObject | null` ready for the native HybridVideoPlayer.
 */
export function resolveVideoSource(source: VideoSource): VideoSourceObject | null {
  if (source === null || source === undefined) {
    return null
  }

  // Bundled local asset (require('./video.mp4'))
  if (typeof source === 'number') {
    const resolved = resolveAssetUri(source)
    return resolved ? { uri: resolved } : null
  }

  // Plain URL string
  if (typeof source === 'string') {
    return { uri: source }
  }

  // VideoSourceObject — resolve assetId to uri if needed
  if (typeof source.assetId === 'number' && !source.uri) {
    const resolved = resolveAssetUri(source.assetId)
    return { ...source, uri: resolved ?? undefined }
  }

  return source
}

// ─────────────────────────────────────────────────────────────────────────────
// resolveAssetUri
//
// Uses React Native's Image.resolveAssetSource (which works on both
// Android and iOS via Metro's asset registry) to turn a require() number
// into a file:// or http://localhost URI.
// ─────────────────────────────────────────────────────────────────────────────

function resolveAssetUri(assetId: number): string | null {
  try {
    const asset = Image.resolveAssetSource(assetId)
    return asset?.uri ?? null
  } catch {
    if (__DEV__) {
      console.warn(
        `[react-native-nitro-video] Failed to resolve asset source for id: ${assetId}`
      )
    }
    return null
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// normalizeUri
//
// On Android, Metro dev server paths may come through as relative paths.
// This is a best-effort normalizer.
// ─────────────────────────────────────────────────────────────────────────────

export function normalizeUri(uri: string | undefined): string | undefined {
  if (!uri) return uri

  // Already absolute
  if (
    uri.startsWith('http://') ||
    uri.startsWith('https://') ||
    uri.startsWith('file://') ||
    uri.startsWith('content://') ||
    uri.startsWith('asset://')
  ) {
    return uri
  }

  // Android: bare path from Metro dev server
  if (Platform.OS === 'android' && uri.startsWith('/')) {
    return `file://${uri}`
  }

  return uri
}
