import React from 'react'
import { requireNativeComponent } from 'react-native'
import type { VideoAirPlayButtonProps } from './VideoAirPlayButton.types'

const NativeVideoAirPlayButton = requireNativeComponent<VideoAirPlayButtonProps>('VideoAirPlayButtonView')

/**
 * A view displaying the `AVRoutePickerView`. Shows a button; when pressed,
 * an AirPlay device picker appears, allowing users to stream the currently
 * playing video to any available AirPlay sink.
 *
 * > Make sure that `player.allowsExternalPlayback` is set to `true`.
 * @platform ios
 */
export function VideoAirPlayButton(props: VideoAirPlayButtonProps) {
  return (
    <NativeVideoAirPlayButton
      {...props}
      style={[{ minWidth: 30, minHeight: 30 }, props.style]}
    />
  )
}

export default VideoAirPlayButton
