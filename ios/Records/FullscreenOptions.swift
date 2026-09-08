import Foundation

public struct FullscreenOptions: Codable, Equatable, Sendable {
  public var enable: Bool = true
  public var orientation: FullscreenOrientation = .default
  public var autoExitOnRotate: Bool = false
  public var keepFullscreenOnPiPStop: KeepFullscreenOnPiPStopBehavior = .autoEnter

  public init(
    enable: Bool = true,
    orientation: FullscreenOrientation = .default,
    autoExitOnRotate: Bool = false,
    keepFullscreenOnPiPStop: KeepFullscreenOnPiPStopBehavior = .autoEnter
  ) {
    self.enable = enable
    self.orientation = orientation
    self.autoExitOnRotate = autoExitOnRotate
    self.keepFullscreenOnPiPStop = keepFullscreenOnPiPStop
  }
}
