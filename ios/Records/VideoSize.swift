import CoreGraphics
import Foundation

public struct VideoSize: Codable, Equatable, Sendable {
  public var width: Int?
  public var height: Int?

  public init(width: Int? = nil, height: Int? = nil) {
    self.width = width
    self.height = height
  }

  public static func from(_ size: CGSize) -> Self {
    return VideoSize(width: Int(size.width), height: Int(size.height))
  }

  public func toCGSize() -> CGSize {
    guard let width, let height, width > 0, height > 0 else {
      return .zero
    }
    return CGSize(width: width, height: height)
  }
}
