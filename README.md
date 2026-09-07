# react-native-nitro-video 🎥

[![npm version](https://img.shields.io/npm/v/react-native-nitro-video.svg?style=flat-square)](https://www.npmjs.com/package/react-native-nitro-video)
[![license](https://img.shields.io/npm/l/react-native-nitro-video.svg?style=flat-square)](https://github.com/aiworks042/react-native-nitro-video/blob/main/LICENSE)
[![Nitro Modules](https://img.shields.io/badge/Nitro%20Modules-0.37-blue.svg?style=flat-square)](https://nitro.margelo.com)
[![React Native](https://img.shields.io/badge/React%20Native-New%20Architecture%20(Fabric)-black.svg?style=flat-square)](https://reactnative.dev)

A high-performance, next-generation video player for **React Native (CLI & Expo Bare)** built on **Nitro Modules**.

It provides an intuitive, **1:1 Expo-style API** (`useVideoPlayer` & `<VideoView />`) backed by **AndroidX Media3 ExoPlayer** (Android) and **AVPlayer** (iOS) through direct **C++ Fabric ShadowNodes**—delivering 60+ FPS playback with zero bridge overhead and zero Expo runtime bloat.

---

## ⚡ Highlights

- 🚀 **Zero-Bridge Nitro Architecture**: Built with native C++ ShadowNodes and JSI bindings for instantaneous playback without dropped frames.
- 🎯 **Expo-Style DX**: Seamless `useVideoPlayer()` hook and `<VideoView />` component for an effortless developer experience.
- 🤖 **AndroidX Media3 1.9.0**: Uses modern Media3 `ExoPlayer` directly, eliminating legacy constructor conflicts and playing nicely with camera libraries.
- 🍎 **iOS AVPlayer**: Pure Swift implementation with accurate 250ms progress tracking and automatic `AVAudioSession` movie playback.
- 🔊 **Smart Audio Focus**: Automatically pauses or ducks when phone calls or other media playback interrupt.
- 📴 **Host Lifecycle Aware**: Automatically pauses when the app goes to background to save battery and system resources.
- ♻️ **Zero Memory Leaks**: Deterministic native player teardown on unmount (`onDropView`) and supports view recycling.

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
No additional setup needed! React Native autolinking will automatically register `NitroVideoPackage`.

---

## 🚀 Quick Start (Expo-Style API)

```tsx
import React from 'react'
import { StyleSheet, View, Text, Pressable } from 'react-native'
import { useVideoPlayer, VideoView } from 'react-native-nitro-video'

export function VideoPostScreen() {
  const player = useVideoPlayer('https://lorem.video/cat_128kbps', (player) => {
    player.loop = true
    player.muted = false
    player.play()
  })

  return (
    <View style={styles.container}>
      <VideoView
        player={player}
        style={styles.video}
        contentFit="cover" // 'contain' | 'cover' | 'fill'
      />

      <View style={styles.controls}>
        <Pressable
          onPress={() => (player.playing ? player.pause() : player.play())}
          style={styles.button}
        >
          <Text style={styles.buttonText}>
            {player.playing ? 'Pause' : 'Play'}
          </Text>
        </Pressable>

        <Pressable
          onPress={() => (player.muted = !player.muted)}
          style={styles.button}
        >
          <Text style={styles.buttonText}>
            {player.muted ? 'Unmute' : 'Mute'}
          </Text>
        </Pressable>

        <Pressable
          onPress={() => player.seekBy(5)}
          style={styles.button}
        >
          <Text style={styles.buttonText}>+5s</Text>
        </Pressable>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  video: {
    flex: 1,
  },
  controls: {
    position: 'absolute',
    bottom: 40,
    left: 20,
    right: 20,
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  button: {
    backgroundColor: 'rgba(255,255,255,0.25)',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  buttonText: {
    color: '#fff',
    fontWeight: '600',
  },
})
```

---

## 🛠️ Alternative: Declarative Component API

If you prefer classic declarative props:

```tsx
import { VideoPlayerView } from 'react-native-nitro-video'

<VideoPlayerView
  source="https://lorem.video/cat_128kbps"
  paused={false}
  muted={false}
  repeat={true}
  resizeMode="cover"
  style={StyleSheet.absoluteFill}
  onLoad={(duration) => console.log('Video duration:', duration)}
  onProgress={(currentTime, duration) => console.log(currentTime, duration)}
  onEnd={() => console.log('Finished')}
  onError={(err) => console.error(err)}
/>
```

---

## 📖 API Reference

### `useVideoPlayer(source, setupCallback?)`

Hook that creates and manages a `VideoPlayer` session.

| Parameter | Type | Description |
|---|---|---|
| `source` | `string \| { uri: string }` | Video URL, local file path (`/storage/...`), or asset object |
| `setupCallback` | `(player: VideoPlayer) => void` | Optional callback invoked immediately after player initialization |

---

### `VideoPlayer` Properties & Methods

#### Properties
| Property | Type | Access | Description |
|---|---|---|---|
| `playing` / `isPlaying` | `boolean` | Readonly | Whether the video is currently playing |
| `loop` | `boolean` | Read / Write | Whether the video should loop indefinitely |
| `muted` | `boolean` | Read / Write | Audio mute toggle |
| `volume` | `number` | Read / Write | Volume level from `0.0` to `1.0` |
| `currentTime` | `number` | Read / Write | Current playback position in seconds (setting seeks!) |
| `duration` | `number` | Readonly | Total duration in seconds |
| `status` | `'idle' \| 'loading' \| 'readyToPlay' \| 'error'` | Readonly | Current playback status |
| `source` | `string` | Read / Write | Current video source |

#### Methods
| Method | Description |
|---|---|
| `play()` | Resumes or starts playback |
| `pause()` | Pauses playback |
| `replay()` | Seeks to 0 and plays |
| `seekBy(seconds)` | Seeks forward or backward relative to current time |
| `replace(source)` | Swaps out the current video source |
| `replaceAsync(source)` | Async variant of replace |
| `addListener(event, listener)` | Subscribes to player events (`statusChange`, `playingChange`, `timeUpdate`, `playToEnd`, `volumeChange`, `mutedChange`) |
| `removeListener(event, listener)` | Unsubscribes an event listener |

---

### `<VideoView />` Props

| Prop | Type | Default | Description |
|---|---|---|---|
| `player` | `VideoPlayer` | *(required)* | `VideoPlayer` instance created via `useVideoPlayer` |
| `contentFit` | `'contain' \| 'cover' \| 'fill'` | `'contain'` | Aspect ratio resizing behavior |
| `style` | `StyleProp<ViewStyle>` | `undefined` | View styling and dimensions |

---

## 📄 License

MIT © [Anis Rangrez](https://github.com/aiworks042)
