import UIKit

public enum FullscreenOrientation: String, Codable, Sendable {
  case landscape
  case portrait
  case landscapeLeft
  case landscapeRight
  case portraitUp
  case portraitDown
  case `default`

  #if !os(tvOS)
  public func toUIInterfaceOrientationMask() -> UIInterfaceOrientationMask {
    switch self {
    case .landscape:
      return .landscape
    case .portrait:
      return [.portrait, .portraitUpsideDown]
    case .landscapeLeft:
      return .landscapeLeft
    case .landscapeRight:
      return .landscapeRight
    case .portraitUp:
      return .portrait
    case .portraitDown:
      return .portraitUpsideDown
    case .default:
      return .all
    }
  }
  #endif
}
