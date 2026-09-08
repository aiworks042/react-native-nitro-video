import CoreMedia
import Foundation

public struct SeekTolerance: Codable, Equatable, Sendable {
  public var toleranceBefore: Double = 0
  public var toleranceAfter: Double = 0

  private let timescale: Int32 = 10_000_000

  public init(toleranceBefore: Double = 0, toleranceAfter: Double = 0) {
    self.toleranceBefore = toleranceBefore
    self.toleranceAfter = toleranceAfter
  }

  public var cmTimeToleranceBefore: CMTime {
    CMTime(value: CMTimeValue(toleranceBefore * Double(timescale)), timescale: timescale)
  }

  public var cmTimeToleranceAfter: CMTime {
    CMTime(value: CMTimeValue(toleranceAfter * Double(timescale)), timescale: timescale)
  }
}
