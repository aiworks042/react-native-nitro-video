import Foundation

public final class VideoAssetTransportRegistry {
  public static let shared = VideoAssetTransportRegistry()

  public static func registerProvider(_ provider: any VideoAssetTransportProvider) {
    shared.registerProvider(provider)
  }

  public static func unregisterProvider(withId identifier: String) {
    shared.unregisterProvider(withId: identifier)
  }

  private struct Entry {
    let registrationOrder: Int
    var provider: any VideoAssetTransportProvider
  }

  private let queue = DispatchQueue(label: "nitro.video.transport.registry")
  private var entries: [Entry] = []
  private var nextRegistrationOrder = 0
  private var defaultProvidersRegistered = false

  private init() {}

  internal static func registerDefaultProviders() {
    shared.registerDefaultProviders()
  }

  internal static func resolveLoadPlan(for videoSource: VideoSource, url: URL) -> VideoAssetLoadPlan? {
    shared.resolveLoadPlan(for: videoSource, url: url)
  }

  public func registerProvider(_ provider: any VideoAssetTransportProvider) {
    queue.sync {
      upsertProvider(provider)
    }
  }

  public func unregisterProvider(withId identifier: String) {
    queue.sync {
      entries.removeAll { $0.provider.identifier == identifier }
    }
  }

  internal func registerDefaultProviders() {
    queue.sync {
      guard !defaultProvidersRegistered else {
        return
      }
      defaultProvidersRegistered = true
      upsertProvider(CacheVideoAssetTransportProvider())
    }
  }

  internal func resolveLoadPlan(for videoSource: VideoSource, url: URL) -> VideoAssetLoadPlan? {
    registerDefaultProviders()

    let sourceDescriptor = VideoAssetSourceDescriptor(videoSource: videoSource, url: url)
    return queue.sync {
      for entry in sortedEntries() {
        if let loadPlan = entry.provider.makeLoadPlan(for: sourceDescriptor) {
          return loadPlan
        }
      }
      return nil
    }
  }

  private func upsertProvider(_ provider: any VideoAssetTransportProvider) {
    if let index = entries.firstIndex(where: { $0.provider.identifier == provider.identifier }) {
      entries[index].provider = provider
      return
    }

    entries.append(Entry(registrationOrder: nextRegistrationOrder, provider: provider))
    nextRegistrationOrder += 1
  }

  private func sortedEntries() -> [Entry] {
    return entries.sorted { lhs, rhs in
      if lhs.provider.priority == rhs.provider.priority {
        return lhs.registrationOrder < rhs.registrationOrder
      }
      return lhs.provider.priority > rhs.provider.priority
    }
  }
}
