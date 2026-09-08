import Foundation

public struct ScrubbingModeOptions: Codable, Equatable, Sendable {
  public var scrubbingModeEnabled: Bool = false

  public init(scrubbingModeEnabled: Bool = false) {
    self.scrubbingModeEnabled = scrubbingModeEnabled
  }

  public init(nitroOptions: NitroScrubbingModeOptions) {
    self.scrubbingModeEnabled = nitroOptions.scrubbingModeEnabled
  }
}
