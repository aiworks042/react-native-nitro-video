import AVKit
import Foundation

public struct VideoAssetLoadPlan {
  public let assetURL: URL
  public let assetOptions: [String: Any]?
  public let reportedContentTypeHint: ContentType?
  public let resourceLoaderDelegate: (any AVAssetResourceLoaderDelegate)?
  public let resourceLoaderQueue: DispatchQueue?
  public let prepareAsset: ((AVURLAsset) async throws -> Void)?
  public let retainedObjects: [AnyObject]
  public let attachErrorHandler: ((@escaping (Error) -> Void) -> Void)?
  public let onAssetDeinit: (() -> Void)?

  public init(
    assetURL: URL,
    assetOptions: [String: Any]? = nil,
    reportedContentTypeHint: ContentType? = nil,
    resourceLoaderDelegate: (any AVAssetResourceLoaderDelegate)? = nil,
    resourceLoaderQueue: DispatchQueue? = nil,
    prepareAsset: ((AVURLAsset) async throws -> Void)? = nil,
    retainedObjects: [AnyObject] = [],
    attachErrorHandler: ((@escaping (Error) -> Void) -> Void)? = nil,
    onAssetDeinit: (() -> Void)? = nil
  ) {
    self.assetURL = assetURL
    self.assetOptions = assetOptions
    self.reportedContentTypeHint = reportedContentTypeHint
    self.resourceLoaderDelegate = resourceLoaderDelegate
    self.resourceLoaderQueue = resourceLoaderQueue
    self.prepareAsset = prepareAsset
    self.retainedObjects = retainedObjects
    self.attachErrorHandler = attachErrorHandler
    self.onAssetDeinit = onAssetDeinit
  }
}
