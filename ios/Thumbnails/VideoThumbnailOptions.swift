import CoreGraphics
import Foundation

public struct VideoThumbnailOptions: Codable, Sendable {
  public static let `default` = VideoThumbnailOptions()

  public var maxWidth: Int = 0
  public var maxHeight: Int = 0

  public init(maxWidth: Int = 0, maxHeight: Int = 0) {
    self.maxWidth = maxWidth
    self.maxHeight = maxHeight
  }

  public func getMaxSize() -> CGSize {
    return CGSize(width: maxWidth, height: maxHeight)
  }
}
