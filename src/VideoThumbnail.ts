/**
 * Represents a video thumbnail that references a native image.
 * Instances of this class represent generated thumbnail metadata and dimensions.
 * @platform android
 * @platform ios
 */
export class VideoThumbnail {
  /** Width of the created thumbnail in pixels. */
  width: number
  /** Height of the created thumbnail in pixels. */
  height: number
  /** The time in seconds at which the thumbnail was to be created. */
  requestedTime: number
  /** The time in seconds at which the thumbnail was actually generated. */
  actualTime: number

  constructor(data: {
    width: number
    height: number
    requestedTime: number
    actualTime: number
  }) {
    this.width = data.width
    this.height = data.height
    this.requestedTime = data.requestedTime
    this.actualTime = data.actualTime
  }
}

export default VideoThumbnail
