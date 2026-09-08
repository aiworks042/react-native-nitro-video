import { useEffect, useRef, useState } from 'react'
import { VideoPlayer } from '../VideoPlayer'
import type { PlayerBuilderOptions, VideoSource } from '../types/VideoPlayer.types'
import { resolveVideoSource } from '../utils/resolveVideoSource'

// ─────────────────────────────────────────────────────────────────────────────
// useVideoPlayer — drop-in replacement for expo-video's hook
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates a `VideoPlayer` that is automatically released when the component unmounts.
 *
 * Mirrors the expo-video `useVideoPlayer` API exactly:
 * - `source` changes trigger `replaceAsync()` on the existing player.
 * - `playerBuilderOptions` changes recreate the player.
 * - `setup` callback runs once after the player is created.
 *
 * @example
 * ```tsx
 * const player = useVideoPlayer('https://example.com/video.mp4', p => {
 *   p.loop = true
 *   p.play()
 * })
 * ```
 */
export function useVideoPlayer(
  source: VideoSource,
  setup?: (player: VideoPlayer) => void,
  playerBuilderOptions?: PlayerBuilderOptions
): VideoPlayer {
  // Stable serialized keys so we only react when values actually change
  const sourceKey = JSON.stringify(resolveVideoSource(source))
  const optionsKey = JSON.stringify(playerBuilderOptions)

  // Recreate counter — bumped when replaceAsync fails
  const [forceRecreateCount, setForceRecreateCount] = useState(0)

  // Track keys from previous render to detect what changed
  const prevSourceKey = useRef<string | null>(null)
  const prevOptionsKey = useRef<string | null>(null)

  // The player instance — recreated only when options or forcedRecreate changes
  const [player, setPlayer] = useState<VideoPlayer>(() => {
    const p = new VideoPlayer(source, false, playerBuilderOptions)
    setup?.(p)
    prevSourceKey.current = sourceKey
    prevOptionsKey.current = optionsKey
    return p
  })

  // ─── React to options change — recreate the player ────────────────────────
  useEffect(() => {
    if (
      prevOptionsKey.current !== null &&
      (prevOptionsKey.current !== optionsKey || forceRecreateCount > 0)
    ) {
      player.release()
      const p = new VideoPlayer(source, false, playerBuilderOptions)
      setup?.(p)
      prevSourceKey.current = sourceKey
      prevOptionsKey.current = optionsKey
      setPlayer(p)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [optionsKey, forceRecreateCount])

  // ─── React to source change — replaceAsync ────────────────────────────────
  useEffect(() => {
    if (prevSourceKey.current !== null && prevSourceKey.current !== sourceKey) {
      prevSourceKey.current = sourceKey
      player.replaceAsync(source).catch(() => {
        // replaceAsync failed — force full player recreation
        setForceRecreateCount((c) => c + 1)
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sourceKey])

  // ─── Cleanup on unmount ───────────────────────────────────────────────────
  useEffect(() => {
    return () => {
      player.release()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [player])

  return player
}

// ─────────────────────────────────────────────────────────────────────────────
// useEventListener — mirrors expo-video's useEventListener helper
// ─────────────────────────────────────────────────────────────────────────────

import type { VideoPlayerEvents } from '../types/VideoPlayerEvents.types'

/**
 * Attaches an event listener to a `VideoPlayer` and automatically removes it
 * when the component unmounts or the player / event / listener changes.
 *
 * @example
 * ```tsx
 * useEventListener(player, 'statusChange', ({ status }) => {
 *   console.log('Status changed:', status)
 * })
 * ```
 */
export function useEventListener<K extends keyof VideoPlayerEvents>(
  player: VideoPlayer | null | undefined,
  event: K,
  listener: VideoPlayerEvents[K]
): void {
  useEffect(() => {
    if (!player) return
    const subscription = player.addListener(event, listener)
    return () => subscription.remove()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [player, event, listener])
}
