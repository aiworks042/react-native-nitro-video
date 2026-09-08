import Foundation

public enum KeepFullscreenOnPiPStopBehavior: String, Codable, Sendable {
  case always
  case autoEnter
  case never

  public func shouldRestore(pipWasAutoEntered: Bool) -> Bool {
    switch self {
    case .always:
      return true
    case .autoEnter:
      return pipWasAutoEntered
    case .never:
      return false
    }
  }
}
