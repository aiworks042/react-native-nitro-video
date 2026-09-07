# react-native-nitro-video 🎥

Ultra-fast, native React Native video player built with **Nitro Modules** (Fabric New Architecture).

Powered by **AndroidX Media3 ExoPlayer** (Android) and **AVPlayer** (iOS) with zero bridge overhead, direct C++ ShadowNodes, and automatic callback boxing.

---

## Features

- ⚡ **Zero-Bridge Nitro View**: Built natively with Fabric C++ ShadowNodes for 60+ FPS fluid playback.
- 📱 **Android Media3 1.9.0**: Modern AndroidX ExoPlayer implementation without legacy constructor conflicts.
- 🍏 **iOS AVPlayer**: Pure Swift `AVPlayerLayer` implementation with accurate time tracking.
- 🔁 **Full Playback Controls**: Play, pause, seek, volume, mute, looping (`repeat`), and aspect ratio modes (`contain`, `cover`, `stretch`).
- 🛡️ **Zero Memory Leaks**: Automatically tears down native player instances on unmount (`onDropView`) and supports view recycling.

---

## Installation

```bash
bun add react-native-nitro-video react-native-nitro-modules
# or
npm install react-native-nitro-video react-native-nitro-modules
```

### iOS Setup
```bash
bundle exec pod install
```

### Android Setup
No additional setup needed! Autolinking automatically registers `NitroVideoPackage`.

---

## Usage

```tsx
import React, { useRef } from 'react'
import { StyleSheet, View } from 'react-native'
import { VideoPlayer, type VideoPlayerRef } from 'react-native-nitro-video'

export function App() {
  const playerRef = useRef<VideoPlayerRef | null>(null)

  return (
    <View style={styles.container}>
      <VideoPlayer
        source="https://lorem.video/cat_128kbps"
        paused={false}
        muted={false}
        repeat={true}
        resizeMode="cover"
        style={StyleSheet.absoluteFill}
        onLoad={(duration) => console.log('Loaded video duration:', duration)}
        onProgress={(currentTime, duration) => console.log('Progress:', currentTime, duration)}
        onEnd={() => console.log('Playback finished')}
        onError={(err) => console.error('Video error:', err)}
        hybridRef={(ref) => {
          playerRef.current = ref
        }}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
})
```

---

## Props

| Prop | Type | Default | Description |
|---|---|---|---|
| `source` | `string` | *(required)* | URL or local file path to the video |
| `paused` | `boolean` | `false` | Pause or resume playback |
| `muted` | `boolean` | `false` | Mute audio output |
| `repeat` | `boolean` | `false` | Loop playback continuously |
| `volume` | `number` | `1.0` | Audio volume (`0.0` to `1.0`) |
| `resizeMode` | `'contain' \| 'cover' \| 'stretch'` | `'contain'` | Aspect ratio resize mode |
| `onLoad` | `(duration: number) => void` | `undefined` | Callback fired when video is ready |
| `onProgress` | `(currentTime: number, duration: number) => void` | `undefined` | Periodic playback progress (every 250ms) |
| `onEnd` | `() => void` | `undefined` | Callback fired when video reaches end |
| `onError` | `(error: string) => void` | `undefined` | Callback fired if an error occurs |
| `hybridRef` | `(ref: VideoPlayerRef) => void` | `undefined` | Imperative handle (`play()`, `pause()`, `seek(sec)`) |

---

## License

MIT
