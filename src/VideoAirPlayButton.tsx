import React from 'react'
import { View } from 'react-native'
import type { VideoAirPlayButtonProps } from './VideoAirPlayButton.types'

/**
 * A view displaying the AirPlay route picker button.
 * On non-iOS platforms, renders an empty placeholder View.
 * @platform ios
 */
export function VideoAirPlayButton(props: VideoAirPlayButtonProps) {
  return <View {...props} style={[{ minWidth: 30, minHeight: 30 }, props.style]} />
}

export default VideoAirPlayButton
