import { NitroModules } from 'react-native-nitro-modules'
import type { VideoPlayer as NativeVideoPlayer } from './specs/HybridVideoPlayer.nitro'
import type {
  AudioMixingMode,
  AudioTrack,
  BufferOptions,
  PlayerBuilderOptions,
  ScrubbingModeOptions,
  SeekTolerance,
  SubtitleTrack,
  VideoPlayerStatus,
  VideoSize,
  VideoSource,
  VideoTrack,
  VideoThumbnail,
  VideoThumbnailOptions,
} from './types/VideoPlayer.types'
import type { VideoPlayerEvents } from './types/VideoPlayerEvents.types'
import { resolveVideoSource } from './utils/resolveVideoSource'

// ─────────────────────────────────────────────────────────────────────────────
// VideoPlayer — mirrors expo-video's VideoPlayer exactly.
// Backed by NitroModules.createHybridObject<NativeVideoPlayer>('VideoPlayer').
// ─────────────────────────────────────────────────────────────────────────────

export class VideoPlayer {
  /** Unique internal id matching the native player id. */
  readonly __nitro_player_id__: number

  /** @internal — the raw Nitro native object */
  private _native: NativeVideoPlayer

  /** @internal — listener cleanup map */
  private _listenerIds: Map<string, number[]> = new Map()

  // Track JS-side states that native handles via setters
  private _bufferOptions: BufferOptions = {}
  private _seekTolerance: SeekTolerance = {}
  private _scrubbingModeOptions: ScrubbingModeOptions = {}
  private _subtitleTrack: SubtitleTrack | null = null
  private _audioTrack: AudioTrack | null = null
  private _maxResolution: VideoSize | null = null

  // Dynamic reactive state updated by native events
  private _availableSubtitleTracks: SubtitleTrack[] = []
  private _availableAudioTracks: AudioTrack[] = []
  private _availableVideoTracks: VideoTrack[] = []
  private _videoTrack: VideoTrack | null = null
  private _currentLiveTimestamp: number | null = null
  private _currentOffsetFromLive: number | null = null

  constructor(
    source: VideoSource,
    _useSynchronousReplace = false,
    _playerBuilderOptions?: PlayerBuilderOptions
  ) {
    this._native = NitroModules.createHybridObject<NativeVideoPlayer>('VideoPlayer')
    this.__nitro_player_id__ = this._native.id

    // Attach internal listeners for dynamic state synchronization
    this._native.addListener('timeUpdate', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        if (typeof p.currentLiveTimestamp === 'number') {
          this._currentLiveTimestamp = p.currentLiveTimestamp
        }
        if (typeof p.currentOffsetFromLive === 'number') {
          this._currentOffsetFromLive = p.currentOffsetFromLive
        }
      } catch {}
    })

    this._native.addListener('sourceLoad', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        if (Array.isArray(p.availableVideoTracks)) {
          this._availableVideoTracks = p.availableVideoTracks
        }
        if (Array.isArray(p.availableSubtitleTracks)) {
          this._availableSubtitleTracks = p.availableSubtitleTracks
        }
        if (Array.isArray(p.availableAudioTracks)) {
          this._availableAudioTracks = p.availableAudioTracks
        }
      } catch {}
    })

    this._native.addListener('availableSubtitleTracksChange', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        if (Array.isArray(p.availableSubtitleTracks)) {
          this._availableSubtitleTracks = p.availableSubtitleTracks
        }
      } catch {}
    })

    this._native.addListener('availableAudioTracksChange', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        if (Array.isArray(p.availableAudioTracks)) {
          this._availableAudioTracks = p.availableAudioTracks
        }
      } catch {}
    })

    this._native.addListener('subtitleTrackChange', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        this._subtitleTrack = p.subtitleTrack ?? null
      } catch {}
    })

    this._native.addListener('audioTrackChange', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        this._audioTrack = p.audioTrack ?? null
      } catch {}
    })

    this._native.addListener('videoTrackChange', (payloadStr: string) => {
      try {
        const p = JSON.parse(payloadStr)
        this._videoTrack = p.videoTrack ?? null
      } catch {}
    })

    const parsed = resolveVideoSource(source)
    const uri = parsed?.uri ?? ''
    const useCaching = parsed?.useCaching ?? false
    const contentType = parsed?.contentType ?? 'auto'
    this._native.load(uri, useCaching, contentType)
  }

  // ─── Readonly state ───────────────────────────────────────────────────────

  get playing(): boolean { return this._native.playing }
  get currentLiveTimestamp(): number | null { return this._currentLiveTimestamp }
  get currentOffsetFromLive(): number | null { return this._currentOffsetFromLive }
  get duration(): number { return this._native.duration }
  get isLive(): boolean { return this._native.isLive }
  get status(): VideoPlayerStatus { return this._native.status as VideoPlayerStatus }
  get bufferedPosition(): number { return this._native.bufferedPosition }
  get availableSubtitleTracks(): SubtitleTrack[] { return this._availableSubtitleTracks }
  get availableAudioTracks(): AudioTrack[] { return this._availableAudioTracks }
  get availableVideoTracks(): VideoTrack[] { return this._availableVideoTracks }
  get videoTrack(): VideoTrack | null { return this._videoTrack }
  get isExternalPlaybackActive(): boolean { return this._native.isExternalPlaybackActive }

  // ─── Read-write properties ────────────────────────────────────────────────

  get loop(): boolean { return this._native.loop }
  set loop(v: boolean) { this._native.loop = v }

  get allowsExternalPlayback(): boolean { return this._native.allowsExternalPlayback }
  set allowsExternalPlayback(v: boolean) { this._native.allowsExternalPlayback = v }

  get audioMixingMode(): AudioMixingMode { return this._native.audioMixingMode as AudioMixingMode }
  set audioMixingMode(v: AudioMixingMode) { this._native.audioMixingMode = v }

  get muted(): boolean { return this._native.muted }
  set muted(v: boolean) { this._native.muted = v }

  get currentTime(): number { return this._native.currentTime }
  set currentTime(v: number) { this._native.currentTime = v }

  get targetOffsetFromLive(): number { return 0 }
  set targetOffsetFromLive(_v: number) {}

  get volume(): number { return this._native.volume }
  set volume(v: number) { this._native.volume = v }

  get preservesPitch(): boolean { return this._native.preservesPitch }
  set preservesPitch(v: boolean) { this._native.preservesPitch = v }

  get timeUpdateEventInterval(): number { return this._native.timeUpdateEventInterval }
  set timeUpdateEventInterval(v: number) { this._native.timeUpdateEventInterval = v }

  get playbackRate(): number { return this._native.playbackRate }
  set playbackRate(v: number) { this._native.playbackRate = v }

  get keepScreenOnWhilePlaying(): boolean { return this._native.keepScreenOnWhilePlaying }
  set keepScreenOnWhilePlaying(v: boolean) { this._native.keepScreenOnWhilePlaying = v }

  get showNowPlayingNotification(): boolean { return this._native.showNowPlayingNotification }
  set showNowPlayingNotification(v: boolean) { this._native.showNowPlayingNotification = v }

  get staysActiveInBackground(): boolean { return this._native.staysActiveInBackground }
  set staysActiveInBackground(v: boolean) { this._native.staysActiveInBackground = v }

  get bufferOptions(): BufferOptions { return this._bufferOptions }
  set bufferOptions(v: BufferOptions) {
    this._bufferOptions = v
    this._native.setBufferOptions({
      preferredForwardBufferDuration: v.preferredForwardBufferDuration ?? 0,
      waitsToMinimizeStalling: v.waitsToMinimizeStalling ?? true,
      minBufferForPlayback: v.minBufferForPlayback ?? 2,
      maxBufferBytes: v.maxBufferBytes ?? 0,
      prioritizeTimeOverSizeThreshold: v.prioritizeTimeOverSizeThreshold ?? false,
    })
  }

  get subtitleTrack(): SubtitleTrack | null { return this._subtitleTrack }
  set subtitleTrack(v: SubtitleTrack | null) {
    this._subtitleTrack = v
    this._native.setSubtitleTrack(v?.id ?? '')
  }

  get audioTrack(): AudioTrack | null { return this._audioTrack }
  set audioTrack(v: AudioTrack | null) {
    this._audioTrack = v
    this._native.setAudioTrack(v?.id ?? '')
  }

  get maxResolution(): VideoSize | null { return this._maxResolution }
  set maxResolution(v: VideoSize | null) {
    this._maxResolution = v
    this._native.setMaxResolution(v?.width ?? 0, v?.height ?? 0)
  }

  get seekTolerance(): SeekTolerance { return this._seekTolerance }
  set seekTolerance(v: SeekTolerance) {
    this._seekTolerance = v
    this._native.setSeekTolerance(v.toleranceBefore ?? 0, v.toleranceAfter ?? 0)
  }

  get scrubbingModeOptions(): ScrubbingModeOptions { return this._scrubbingModeOptions }
  set scrubbingModeOptions(v: ScrubbingModeOptions) {
    this._scrubbingModeOptions = v
    this._native.setScrubbingModeOptions({
      scrubbingModeEnabled: v.scrubbingModeEnabled ?? false,
      increaseCodecOperatingRate: v.increaseCodecOperatingRate ?? true,
      enableDynamicScheduling: v.enableDynamicScheduling ?? true,
      useDecodeOnlyFlag: v.useDecodeOnlyFlag ?? true,
      allowSkippingMediaCodecFlush: v.allowSkippingMediaCodecFlush ?? true,
    })
  }

  // ─── Playback control ─────────────────────────────────────────────────────

  play(): void {
    this._native.play()
  }

  pause(): void {
    this._native.pause()
  }

  /**
   * @deprecated Use `replaceAsync` instead.
   */
  replace(source: VideoSource, disableWarning = false): void {
    if (!disableWarning) {
      console.warn(
        '[react-native-nitro-video] VideoPlayer.replace() is deprecated. ' +
          'Use replaceAsync() instead.'
      )
    }
    const parsed = resolveVideoSource(source)
    this._native.load(parsed?.uri ?? '', parsed?.useCaching ?? false, parsed?.contentType ?? 'auto')
  }

  async replaceAsync(source: VideoSource): Promise<void> {
    const parsed = resolveVideoSource(source)
    return this._native.replaceAsync(parsed?.uri ?? '')
  }

  seekBy(seconds: number): void {
    this._native.seekBy(seconds)
  }

  replay(): void {
    this._native.replay()
  }

  async generateThumbnailsAsync(
    times: number | number[],
    options?: VideoThumbnailOptions
  ): Promise<VideoThumbnail[]> {
    const timesArray = Array.isArray(times) ? times : [times]
    return this._native.generateThumbnailsAsync(
      timesArray,
      options?.maxWidth ?? 0,
      options?.maxHeight ?? 0
    ) as Promise<VideoThumbnail[]>
  }

  // ─── Event emitter ────────────────────────────────────────────────────────

  addListener<K extends keyof VideoPlayerEvents>(
    event: K,
    listener: VideoPlayerEvents[K]
  ): { remove: () => void } {
    const id = this._native.addListener(event as string, (payloadStr: string) => {
      try {
        const payload = payloadStr ? JSON.parse(payloadStr) : {}
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ;(listener as any)(payload)
      } catch {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ;(listener as any)({})
      }
    })
    const existing = this._listenerIds.get(event as string) ?? []
    this._listenerIds.set(event as string, [...existing, id])
    return {
      remove: () => this._native.removeListener(id),
    }
  }

  removeAllListeners<K extends keyof VideoPlayerEvents>(event?: K): void {
    if (event) {
      const ids = this._listenerIds.get(event as string) ?? []
      ids.forEach((id) => this._native.removeListener(id))
      this._listenerIds.delete(event as string)
    } else {
      this._listenerIds.forEach((ids) => {
        ids.forEach((id) => this._native.removeListener(id))
      })
      this._listenerIds.clear()
    }
  }

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  release(): void {
    this.removeAllListeners()
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// createVideoPlayer — non-hook factory
// ─────────────────────────────────────────────────────────────────────────────

export function createVideoPlayer(
  source: VideoSource,
  playerBuilderOptions?: PlayerBuilderOptions
): VideoPlayer {
  return new VideoPlayer(source, false, playerBuilderOptions)
}
