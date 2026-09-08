# react-native-nitro-video 🎥

[![npm version](https://img.shields.io/npm/v/react-native-nitro-video.svg?style=flat-square)](https://www.npmjs.com/package/react-native-nitro-video)
[![license](https://img.shields.io/npm/l/react-native-nitro-video.svg?style=flat-square)](https://github.com/aiworks042/react-native-nitro-video/blob/main/LICENSE)
[![Nitro Modules](https://img.shields.io/badge/Nitro%20Modules-0.37-blue.svg?style=flat-square)](https://nitro.margelo.com)
[![React Native](https://img.shields.io/badge/React%20Native-New%20Architecture%20(Fabric)-black.svg?style=flat-square)](https://reactnative.dev)

A high-performance, next-generation video player for **React Native CLI** (and bare Expo) powered by **Nitro Modules** (C++ JSI).

`react-native-nitro-video` is a **100% drop-in replacement for `expo-video`** designed specifically for React Native CLI. It provides the exact same intuitive API (`useVideoPlayer`, `<VideoView />`, and `<VideoAirPlayButton />`) backed by **AndroidX Media3 ExoPlayer** (Android) and **AVPlayer** (iOS), completely eliminating `@expo/modules-core` / `ExpoModulesCore` runtime overhead while delivering instantaneous synchronous property reads and 60+ FPS playback.

---

## ⚡ Key Features

* 🚀 **Zero-Bridge Nitro Architecture**: Powered by C++ JSI bindings for synchronous property reads (`player.playing`, `player.duration`, `player.currentTime`) with zero serialization lag.
* 🎯 **100% `expo-video` Parity**: Same hooks (`useVideoPlayer`, `useEventListener`), components (`<VideoView />`, `<VideoAirPlayButton />`), and types. Just swap your import!
* 🤖 **AndroidX Media3 ExoPlayer**: Latest Media3 engine with adaptive bitrate streaming (HLS, DASH, SmoothStreaming), custom load controls, and decoder optimizations.
* 🍎 **iOS AVPlayer Engine**: Full AVKit & AVFoundation integration featuring the **Apple QA1820 smooth seeking chase algorithm**, avoiding frame drops during rapid scrubbing.
* 💾 **Multi-Variant Offline Caching**: RFC 9111 HTTP cache compliance with byte-range interval tracking across both Android (`SimpleCache`) and iOS (`ResourceLoaderDelegate`).
* 🔐 **Hardware DRM Protection**: Widevine, ClearKey, and PlayReady on Android; FairPlay Streaming with asynchronous SPC/CKC exchanges on iOS.
* 🖼️ **Frame-Accurate Thumbnails**: Asynchronous thumbnail generator (`generateThumbnailsAsync`) supporting both iOS 16+ async sequences and Android frame extractors.
* 📺 **Picture-in-Picture (PiP) & Fullscreen**: Native PiP with Android 12+ automatic background transitions, multi-orientation fullscreen locking, and auto-exit on device rotation.
* 🔔 **Lock Screen & Background Audio**: Background audio playback policies with Lock Screen and Dynamic Island controls via `MPNowPlayingInfoCenter` and Android `MediaSessionService`.
* 📡 **AirPlay Support**: Built-in `<VideoAirPlayButton />` wrapping Apple's `AVRoutePickerView`.

---

## 📦 Installation

```bash
bun add react-native-nitro-video react-native-nitro-modules
# or
npm install react-native-nitro-video react-native-nitro-modules
# or
yarn add react-native-nitro-video react-native-nitro-modules
```

### iOS Setup
Run CocoaPods installation:
```bash
cd ios && bundle exec pod install
```

### Android Setup
No additional setup needed! React Native autolinking automatically registers `NitroVideoPackage`.

---

## 🔄 Migrating from `expo-video`

`react-native-nitro-video` was built to be a drop-in replacement. Simply replace your import statement:

```diff
- import { VideoView, useVideoPlayer, useEventListener } from 'expo-video';
+ import { VideoView, useVideoPlayer, useEventListener } from 'react-native-nitro-video';
```

All method signatures, props, callbacks, and event payloads are identical.

---

## 🚀 Quick Start

```tsx
import React from 'react';
import { StyleSheet, View, Text, Pressable } from 'react-native';
import { useVideoPlayer, VideoView } from 'react-native-nitro-video';

export function VideoScreen() {
  const player = useVideoPlayer('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4', (player) => {
    player.loop = true;
    player.muted = false;
    player.play();
  });

  return (
    <View style={styles.container}>
      <VideoView
        player={player}
        style={styles.video}
        contentFit="contain"
        nativeControls
        allowsPictureInPicture
        startsPictureInPictureAutomatically
      />

      <View style={styles.controls}>
        <Pressable
          onPress={() => (player.playing ? player.pause() : player.play())}
          style={styles.button}
        >
          <Text style={styles.buttonText}>{player.playing ? 'Pause' : 'Play'}</Text>
        </Pressable>

        <Pressable
          onPress={() => (player.muted = !player.muted)}
          style={styles.button}
        >
          <Text style={styles.buttonText}>{player.muted ? 'Unmute' : 'Mute'}</Text>
        </Pressable>

        <Pressable
          onPress={() => player.seekBy(10)}
          style={styles.button}
        >
          <Text style={styles.buttonText}>+10s</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
  },
  video: {
    width: '100%',
    height: 300,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 16,
    marginTop: 20,
  },
  button: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  buttonText: {
    color: '#fff',
    fontWeight: '600',
  },
});
```

---

## 📖 API Reference

### `useVideoPlayer(source, setup?, playerBuilderOptions?)`

A React hook that creates and manages a `VideoPlayer` instance, automatically releasing native resources when the component unmounts.

```tsx
const player = useVideoPlayer(source, (player) => {
  player.loop = true;
  player.play();
});
```

* **`source`**: `string | number | VideoSourceObject | null`
  * URL string (e.g. `'https://...'`)
  * Local required asset (e.g. `require('./assets/video.mp4')`)
  * Source object with headers, caching, or DRM options:
    ```tsx
    const player = useVideoPlayer({
      uri: 'https://example.com/stream.m3u8',
      useCaching: true,
      headers: { Authorization: 'Bearer token' },
      drm: {
        type: 'fairplay',
        licenseServer: 'https://license.example.com',
        certificateUrl: 'https://cert.example.com',
      },
    });
    ```
* **`setup`**: `(player: VideoPlayer) => void` (Optional callback run once on player initialization)
* **`playerBuilderOptions`**: `PlayerBuilderOptions` (Optional Android frame rate and seek increment options)

---

### `createVideoPlayer(source, playerBuilderOptions?)`

Creates a standalone `VideoPlayer` instance outside of React component lifecycles. Remember to call `player.release()` when finished.

---

### `VideoPlayer`

#### Properties

| Property | Type | Access | Description |
| :--- | :--- | :--- | :--- |
| `playing` | `boolean` | Readonly | Whether the player is currently playing |
| `duration` | `number` | Readonly | Total duration of the media in seconds |
| `currentTime` | `number` | Read / Write | Current playback position in seconds (setting seeks the player) |
| `status` | `'idle' \| 'loading' \| 'readyToPlay' \| 'error'` | Readonly | Current player lifecycle status |
| `bufferedPosition` | `number` | Readonly | Buffered position in seconds |
| `isLive` | `boolean` | Readonly | Whether the active source is a live stream |
| `currentLiveTimestamp` | `number \| null` | Readonly | Epoch timestamp from HLS `EXT-X-PROGRAM-DATE-TIME` |
| `currentOffsetFromLive` | `number \| null` | Readonly | Latency from the live stream edge in seconds |
| `isExternalPlaybackActive`| `boolean` | Readonly | Whether the player is streaming via AirPlay |
| `loop` | `boolean` | Read / Write | Whether the player should automatically loop |
| `muted` | `boolean` | Read / Write | Audio mute state |
| `volume` | `number` | Read / Write | Audio volume from `0.0` to `1.0` |
| `playbackRate` | `number` | Read / Write | Playback speed multiplier (e.g. `0.5`, `1.0`, `2.0`) |
| `preservesPitch` | `boolean` | Read / Write | Whether to maintain audio pitch during speed changes |
| `audioMixingMode` | `AudioMixingMode` | Read / Write | `'auto' \| 'mixWithOthers' \| 'duckOthers' \| 'doNotMix'` |
| `showNowPlayingNotification`| `boolean` | Read / Write | Enables Lock Screen / Dynamic Island controls |
| `staysActiveInBackground` | `boolean` | Read / Write | Enables continuous background audio playback |
| `keepScreenOnWhilePlaying`| `boolean` | Read / Write | Keeps device screen awake during playback |
| `timeUpdateEventInterval` | `number` | Read / Write | Interval in seconds for `timeUpdate` events (0 = disabled) |
| `availableSubtitleTracks` | `SubtitleTrack[]` | Readonly | Available subtitle/caption tracks |
| `subtitleTrack` | `SubtitleTrack \| null` | Read / Write | Active subtitle track |
| `availableAudioTracks` | `AudioTrack[]` | Readonly | Available audio language tracks |
| `audioTrack` | `AudioTrack \| null` | Read / Write | Active audio track |
| `availableVideoTracks` | `VideoTrack[]` | Readonly | Available video resolutions and bitrates |
| `videoTrack` | `VideoTrack \| null` | Readonly | Active video track |

#### Methods

* `play(): void` — Starts or resumes playback.
* `pause(): void` — Pauses playback.
* `replay(): void` — Seeks to 0 and resumes playback.
* `seekBy(seconds: number): void` — Seeks relative to the current position.
* `replaceAsync(source: VideoSource): Promise<void>` — Seamlessly swaps the video source without re-instantiating native player objects.
* `generateThumbnailsAsync(times: number | number[], options?: VideoThumbnailOptions): Promise<VideoThumbnail[]>` — Generates frame-accurate thumbnails.
* `addListener(event, callback): { remove: () => void }` — Subscribes to player events.
* `removeAllListeners(event?): void` — Cleans up event listeners.
* `release(): void` — Destroys the native player session.

---

### `<VideoView />`

| Prop | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `player` | `VideoPlayer` | *(required)* | `VideoPlayer` instance |
| `contentFit` | `'contain' \| 'cover' \| 'fill'` | `'contain'` | Video aspect ratio scaling behavior |
| `nativeControls` | `boolean` | `true` | Displays native platform playback controls |
| `allowsPictureInPicture` | `boolean` | `false` | Enables Picture-in-Picture support |
| `startsPictureInPictureAutomatically` | `boolean` | `false` | Automatically enters PiP when app is backgrounded |
| `requiresLinearPlayback` | `boolean` | `false` | Disables scrubbing/seeking in native controls |
| `showsTimecodes` | `boolean` | `true` | Shows/hides timecode labels in controls (iOS) |
| `useExoShutter` | `boolean` | `false` | Uses ExoPlayer shutter view during loading (Android) |
| `onPictureInPictureStart` | `() => void` | `undefined` | Callback fired when PiP starts |
| `onPictureInPictureStop` | `() => void` | `undefined` | Callback fired when PiP exits |
| `onFullscreenEnter` | `() => void` | `undefined` | Callback fired when fullscreen mode opens |
| `onFullscreenExit` | `() => void` | `undefined` | Callback fired when fullscreen mode closes |
| `onFirstFrameRender` | `() => void` | `undefined` | Callback fired when the first video frame is drawn |

#### Imperative Ref Methods
```tsx
const videoViewRef = useRef<VideoView>(null);

await videoViewRef.current?.enterFullscreen();
await videoViewRef.current?.exitFullscreen();
await videoViewRef.current?.startPictureInPicture();
await videoViewRef.current?.stopPictureInPicture();
```

---

### `<VideoAirPlayButton />`

Displays Apple's native `AVRoutePickerView` AirPlay sink picker on iOS.

```tsx
import { VideoAirPlayButton } from 'react-native-nitro-video';

<VideoAirPlayButton
  tint="#ffffff"
  activeTint="#007aff"
  prioritizeVideoDevices
  style={{ width: 32, height: 32 }}
/>
```

---

### `useEventListener(player, event, listener)`

A React hook that attaches a typed listener to a `VideoPlayer` and automatically detaches it when the component unmounts.

```tsx
useEventListener(player, 'playingChange', ({ isPlaying }) => {
  console.log('Is playing:', isPlaying);
});

useEventListener(player, 'timeUpdate', ({ currentTime, bufferedPosition }) => {
  console.log(`Progress: ${currentTime}s / Buffered: ${bufferedPosition}s`);
});

useEventListener(player, 'statusChange', ({ status, error }) => {
  if (status === 'error') {
    console.error('Playback error:', error?.message);
  }
});
```

---

### Module-Level Cache Management

Manage offline video storage quotas across both platforms:

```tsx
import {
  isPictureInPictureSupported,
  clearVideoCacheAsync,
  setVideoCacheSizeAsync,
  getCurrentVideoCacheSize,
} from 'react-native-nitro-video';

// Check PiP hardware support
const canPiP = isPictureInPictureSupported();

// Set cache limit to 2 GB
await setVideoCacheSizeAsync(2 * 1024 * 1024 * 1024);

// Get current disk usage in bytes
const bytesUsed = getCurrentVideoCacheSize();

// Purge all cached video files
await clearVideoCacheAsync();
```

---

## 📄 License

MIT © [Anis Rangrez](https://github.com/aiworks042)
