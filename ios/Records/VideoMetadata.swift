import Foundation

public struct VideoMetadata: Codable, Equatable, Sendable {
  public var title: String?
  public var artist: String?
  public var artwork: URL?

  public init(title: String? = nil, artist: String? = nil, artwork: URL? = nil) {
    self.title = title
    self.artist = artist
    self.artwork = artwork
  }
}
