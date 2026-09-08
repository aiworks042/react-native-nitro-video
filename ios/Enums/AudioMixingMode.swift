import AVFoundation
import Foundation

public extension AudioMixingMode {
  func priority() -> Int {
    switch self {
    case .donotmix:
      return 3
    case .auto:
      return 2
    case .duckothers:
      return 1
    case .mixwithothers:
      return 0
    }
  }

  func toSessionCategoryOption() -> AVAudioSession.CategoryOptions? {
    switch self {
    case .duckothers:
      return .duckOthers
    case .mixwithothers:
      return .mixWithOthers
    case .donotmix, .auto:
      return nil
    }
  }
}
