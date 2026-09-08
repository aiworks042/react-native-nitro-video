import Foundation

private let defaultCause = "unknown cause"

open class VideoException: LocalizedError, CustomStringConvertible {
  public let message: String
  public init(_ message: String = defaultCause) {
    self.message = message
  }
  open var errorDescription: String? { message }
  open var description: String { message }
}

public final class PictureInPictureUnsupportedException: VideoException {
  public init() {
    super.init("Picture in picture is not supported on this device")
  }
}

public final class DRMUnsupportedException: VideoException {
  public init(_ drmType: String) {
    super.init("DRMType: `\(drmType)` is unsupported on iOS")
  }
}

public final class DRMLoadException: VideoException {
  public init(_ cause: String? = nil) {
    super.init("Failed to decrypt the video stream: \(cause ?? defaultCause)")
  }
}

public final class PlayerException: VideoException {
  public init(_ cause: String? = nil) {
    super.init("Failed to initialise the player: \(cause ?? defaultCause)")
  }
}

public final class PlayerItemLoadException: VideoException {
  public init(_ cause: String? = nil) {
    super.init("Failed to load the player item: \(cause ?? defaultCause)")
  }
}

public final class CachingAssetInitializationException: VideoException {
  public init(_ url: URL?) {
    super.init("Failed to initialize a caching asset. The provided url: \(url?.absoluteString ?? "nil") doesn't have a valid scheme for caching")
  }
}

public final class VideoCacheException: VideoException {
  public init(_ cause: String? = nil) {
    super.init(cause ?? "Unexpected expo-video cache error")
  }
}

public final class VideoCacheUnsupportedFormatException: VideoException {
  public init(_ mimeType: String) {
    super.init("The server responded with a resource with mimeType: \(mimeType) which cannot be played with caching enabled")
  }
}
