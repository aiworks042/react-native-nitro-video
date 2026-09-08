import Foundation

public struct BufferOptions: Codable, Equatable, Sendable {
  public var preferredForwardBufferDuration: Double = 0
  public var waitsToMinimizeStalling: Bool = true

  public init(preferredForwardBufferDuration: Double = 0, waitsToMinimizeStalling: Bool = true) {
    self.preferredForwardBufferDuration = preferredForwardBufferDuration
    self.waitsToMinimizeStalling = waitsToMinimizeStalling
  }

  public init(nitroOptions: NitroBufferOptions) {
    self.preferredForwardBufferDuration = nitroOptions.preferredForwardBufferDuration
    self.waitsToMinimizeStalling = nitroOptions.waitsToMinimizeStalling
  }
}
