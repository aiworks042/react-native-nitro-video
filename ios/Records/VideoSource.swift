import Foundation

public struct VideoSource: Codable, Equatable, Sendable {
  public var uri: URL?
  public var drm: DRMOptions?
  public var metadata: VideoMetadata?
  public var headers: [String: String]?
  public var useCaching: Bool
  public var contentType: ContentType

  public init(
    uri: URL? = nil,
    drm: DRMOptions? = nil,
    metadata: VideoMetadata? = nil,
    headers: [String: String]? = nil,
    useCaching: Bool = false,
    contentType: ContentType = .auto
  ) {
    self.uri = uri
    self.drm = drm
    self.metadata = metadata
    self.headers = headers
    self.useCaching = useCaching
    self.contentType = contentType
  }
}
