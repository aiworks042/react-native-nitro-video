import Foundation

public enum DRMType: String, Codable, Sendable {
  case clearkey
  case fairplay
  case playready
  case widevine

  public func isSupported() -> Bool {
    return self == .fairplay
  }

  public func assertIsSupported() throws {
    if !isSupported() {
      throw DRMUnsupportedException(self.rawValue)
    }
  }
}
