import AVKit
import Foundation

public extension VideoContentFit {
  func toVideoGravity() -> AVLayerVideoGravity {
    switch self {
    case .contain:
      return .resizeAspect
    case .cover:
      return .resizeAspectFill
    case .fill:
      return .resize
    }
  }
}
