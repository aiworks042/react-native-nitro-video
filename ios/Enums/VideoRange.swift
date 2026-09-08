import Foundation

public enum AVVideoRange: String, Codable, Sendable {
  case sdr
  case hlg
  case pq
}

public enum VideoRange: String, Codable, Sendable {
  // Standard dynamic range
  case sdr
  // Hybrid Log-Gamma - HDR backward-compatible with SDR displays
  case hlg
  // Perceptual Quantizer - Formats like HDR10 and Dolby Vision
  case pq

  public static func from(videoRange range: AVVideoRange) -> VideoRange {
    switch range {
    case .hlg:
      return .hlg
    case .pq:
      return .pq
    default:
      return .sdr
    }
  }
}
