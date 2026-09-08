import Foundation

/**
 Global actor for video source and track loading.
 Offloads AVURLAsset creation, property loading, and M3U8 parsing from the main UI thread.
 */
@globalActor
internal actor VideoLoadingActor {
  static let shared = VideoLoadingActor()

  private nonisolated let executor = DispatchQueueSerialExecutor(
    queue: DispatchQueue(label: "nitro.video.loading", qos: .userInitiated)
  )

  nonisolated var unownedExecutor: UnownedSerialExecutor {
    executor.asUnownedSerialExecutor()
  }
}

internal final class DispatchQueueSerialExecutor: SerialExecutor {
  private let queue: DispatchQueue

  init(queue: DispatchQueue) {
    self.queue = queue
  }

  func enqueue(_ job: UnownedJob) {
    queue.async {
      job.runSynchronously(on: self.asUnownedSerialExecutor())
    }
  }

  func asUnownedSerialExecutor() -> UnownedSerialExecutor {
    UnownedSerialExecutor(ordinary: self)
  }
}
