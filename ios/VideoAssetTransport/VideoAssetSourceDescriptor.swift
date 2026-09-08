import Foundation

public struct VideoAssetSourceDescriptor {
  public let url: URL
  public let headers: [String: String]?
  public let usesCaching: Bool
  public let hasDRM: Bool
  public let contentTypeHint: ContentType

  internal init(videoSource: VideoSource, url: URL) {
    self.url = url
    self.headers = videoSource.headers
    self.usesCaching = videoSource.useCaching
    self.hasDRM = videoSource.drm != nil
    self.contentTypeHint = videoSource.contentType
  }
}
