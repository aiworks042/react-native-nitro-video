import Foundation

public protocol VideoAssetTransportProvider: AnyObject {
  var identifier: String { get }
  var priority: Int { get }
  func makeLoadPlan(for source: VideoAssetSourceDescriptor) -> VideoAssetLoadPlan?
}
