import Foundation

public struct PlaybackError: Codable, Equatable, Sendable {
  public var message: String?

  public init(message: String? = nil) {
    self.message = message
  }
}
