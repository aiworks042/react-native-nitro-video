import Foundation

internal class DangerousPropertiesStore {
  var ownerIsReplacing: Bool = false
  var currentTime: Double?

  func applyProperties(to player: HybridVideoPlayer, reset shouldReset: Bool = true) {
    if let currentTime {
      player.currentTime = currentTime
    }
    if shouldReset {
      reset()
    }
  }

  func reset() {
    currentTime = nil
  }
}
