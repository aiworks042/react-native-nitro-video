import CoreGraphics
import CoreMedia
import Foundation
import UIKit

public struct VideoThumbnail {
  public let image: UIImage
  public let requestedTime: CMTime
  public let actualTime: CMTime

  public init(_ cgImage: CGImage, requestedTime: CMTime, actualTime: CMTime) {
    self.image = UIImage(cgImage: cgImage)
    self.requestedTime = requestedTime
    self.actualTime = actualTime
  }

  public func toNitroThumbnail() -> NitroVideoThumbnail {
    return NitroVideoThumbnail(
      width: Double(image.size.width),
      height: Double(image.size.height),
      requestedTime: CMTimeGetSeconds(requestedTime),
      actualTime: CMTimeGetSeconds(actualTime)
    )
  }
}
