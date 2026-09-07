import { useEffect, useRef, useState } from 'react'
import type { VideoPlayerRef } from '../views/VideoPlayer'

export type VideoSource =
  | string
  | { uri?: string; headers?: Record<string, string> }
  | null
  | undefined

export function parseSource(source: VideoSource): string {
  if (typeof source === 'string') return source
  if (source && typeof source === 'object' && source.uri) return source.uri
  return ''
}

export type VideoPlayerStatus = 'idle' | 'loading' | 'readyToPlay' | 'error'

export interface StatusChangeEventPayload {
  status: VideoPlayerStatus
  oldStatus?: VideoPlayerStatus
  error?: string
}

export interface PlayingChangeEventPayload {
  isPlaying: boolean
}

export interface TimeUpdateEventPayload {
  currentTime: number
  duration: number
}

export interface VolumeChangeEventPayload {
  volume: number
}

export interface MutedChangeEventPayload {
  muted: boolean
}

export type VideoPlayerEvents = {
  statusChange: (payload: StatusChangeEventPayload) => void
  playingChange: (payload: PlayingChangeEventPayload) => void
  timeUpdate: (payload: TimeUpdateEventPayload) => void
  playToEnd: () => void
  volumeChange: (payload: VolumeChangeEventPayload) => void
  mutedChange: (payload: MutedChangeEventPayload) => void
}

export interface EventSubscription {
  remove(): void
}

/**
 * Controller instance representing an individual video player session,
 * 100% modeled after the modern Expo VideoPlayer API, powered by Nitro Modules.
 */
export class VideoPlayer {
  private _source: string
  private _playing: boolean = false
  private _muted: boolean = false
  private _loop: boolean = false
  private _volume: number = 1.0
  private _currentTime: number = 0
  private _duration: number = 0
  private _status: VideoPlayerStatus = 'idle'
  private _nativeRef: VideoPlayerRef | null = null
  private _listeners: { [K in keyof VideoPlayerEvents]?: Set<Function> } = {}
  private _changeSubscribers: Set<() => void> = new Set()

  constructor(
    source: VideoSource,
    setup?: (player: VideoPlayer) => void
  ) {
    this._source = parseSource(source)
    if (this._source) {
      this._status = 'loading'
    }
    if (setup) {
      setup(this)
    }
  }

  // --- Getters & Setters ---

  get source(): string {
    return this._source
  }

  set source(newSource: VideoSource) {
    this.replace(newSource)
  }

  get playing(): boolean {
    return this._playing
  }

  get isPlaying(): boolean {
    return this._playing
  }

  get muted(): boolean {
    return this._muted
  }

  set muted(value: boolean) {
    if (this._muted !== value) {
      this._muted = value
      this._emit('mutedChange', { muted: value })
      this._notifyChange()
    }
  }

  get loop(): boolean {
    return this._loop
  }

  set loop(value: boolean) {
    if (this._loop !== value) {
      this._loop = value
      this._notifyChange()
    }
  }

  get volume(): number {
    return this._volume
  }

  set volume(value: number) {
    const clamped = Math.max(0, Math.min(1, value))
    if (this._volume !== clamped) {
      this._volume = clamped
      this._emit('volumeChange', { volume: clamped })
      this._notifyChange()
    }
  }

  get currentTime(): number {
    return this._currentTime
  }

  set currentTime(positionInSeconds: number) {
    const clamped = Math.max(0, positionInSeconds)
    this._currentTime = clamped
    this._nativeRef?.seek(clamped)
    this._notifyChange()
  }

  get duration(): number {
    return this._duration
  }

  get status(): VideoPlayerStatus {
    return this._status
  }

  // --- Public Control Methods ---

  play(): void {
    if (!this._playing) {
      this._playing = true
      this._nativeRef?.play()
      this._emit('playingChange', { isPlaying: true })
      this._notifyChange()
    }
  }

  pause(): void {
    if (this._playing) {
      this._playing = false
      this._nativeRef?.pause()
      this._emit('playingChange', { isPlaying: false })
      this._notifyChange()
    }
  }

  replay(): void {
    this.currentTime = 0
    this.play()
  }

  seekBy(seconds: number): void {
    this.currentTime = Math.max(0, this._currentTime + seconds)
  }

  replace(newSource: VideoSource): void {
    const parsed = parseSource(newSource)
    if (this._source !== parsed) {
      const oldStatus = this._status
      this._source = parsed
      this._currentTime = 0
      this._duration = 0
      this._status = parsed ? 'loading' : 'idle'
      this._emit('statusChange', { status: this._status, oldStatus })
      this._notifyChange()
    }
  }

  async replaceAsync(newSource: VideoSource): Promise<void> {
    this.replace(newSource)
  }

  // --- Event Listeners ---

  addListener<K extends keyof VideoPlayerEvents>(
    event: K,
    listener: VideoPlayerEvents[K]
  ): EventSubscription {
    if (!this._listeners[event]) {
      this._listeners[event] = new Set()
    }
    this._listeners[event]!.add(listener as Function)

    return {
      remove: () => {
        this.removeListener(event, listener)
      },
    }
  }

  removeListener<K extends keyof VideoPlayerEvents>(
    event: K,
    listener: VideoPlayerEvents[K]
  ): void {
    this._listeners[event]?.delete(listener as Function)
  }

  // --- Internal / View Bridge Methods ---

  /** @internal */
  _attachNativeRef(ref: VideoPlayerRef | null): void {
    this._nativeRef = ref
    if (ref && this._playing) {
      ref.play()
    }
  }

  /** @internal */
  _onLoad(duration: number): void {
    const oldStatus = this._status
    this._duration = duration
    this._status = 'readyToPlay'
    this._emit('statusChange', { status: 'readyToPlay', oldStatus })
    this._notifyChange()
  }

  /** @internal */
  _onProgress(currentTime: number, duration: number): void {
    this._currentTime = currentTime
    this._duration = duration
    this._emit('timeUpdate', { currentTime, duration })
    this._notifyChange()
  }

  /** @internal */
  _onEnd(): void {
    if (!this._loop) {
      this._playing = false
      this._emit('playingChange', { isPlaying: false })
    }
    this._emit('playToEnd')
    this._notifyChange()
  }

  /** @internal */
  _onError(error: string): void {
    const oldStatus = this._status
    this._status = 'error'
    this._playing = false
    this._emit('statusChange', { status: 'error', oldStatus, error })
    this._notifyChange()
  }

  /** @internal */
  _subscribe(listener: () => void): () => void {
    this._changeSubscribers.add(listener)
    return () => {
      this._changeSubscribers.delete(listener)
    }
  }

  private _notifyChange(): void {
    this._changeSubscribers.forEach((cb) => cb())
  }

  private _emit<K extends keyof VideoPlayerEvents>(
    event: K,
    ...args: Parameters<VideoPlayerEvents[K]>
  ): void {
    const set = this._listeners[event]
    if (set) {
      set.forEach((fn) => (fn as any)(...args))
    }
  }
}

export type VideoPlayerInstance = VideoPlayer

/**
 * Creates a standalone `VideoPlayer` instance.
 */
export function createVideoPlayer(
  source: VideoSource,
  setup?: (player: VideoPlayer) => void
): VideoPlayer {
  return new VideoPlayer(source, setup)
}

/**
 * Modern React hook mirroring Expo's `useVideoPlayer`.
 * Automatically manages video state and synchronization with `<VideoView />`.
 *
 * @example
 * ```tsx
 * const player = useVideoPlayer('https://lorem.video/cat_128kbps', (player) => {
 *   player.loop = true;
 *   player.play();
 * });
 *
 * return <VideoView player={player} style={styles.video} contentFit="cover" />;
 * ```
 */
export function useVideoPlayer(
  source: VideoSource,
  setup?: (player: VideoPlayer) => void
): VideoPlayer {
  const [, forceUpdate] = useState(0)
  const playerRef = useRef<VideoPlayer | null>(null)
  const sourceString = parseSource(source)

  if (!playerRef.current) {
    playerRef.current = new VideoPlayer(source, setup)
  }

  useEffect(() => {
    const player = playerRef.current
    if (player && sourceString !== player.source) {
      player.replace(source)
    }
  }, [sourceString, source])

  useEffect(() => {
    const player = playerRef.current
    if (!player) return

    const unsubscribe = player._subscribe(() => {
      forceUpdate((c) => c + 1)
    })
    return () => {
      unsubscribe()
    }
  }, [])

  return playerRef.current
}
