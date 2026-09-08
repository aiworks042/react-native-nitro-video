import Foundation

public enum ContentType: String, Codable, Sendable {
  case auto
  case progressive
  case hls
  case dash
  case smoothStreaming
}
