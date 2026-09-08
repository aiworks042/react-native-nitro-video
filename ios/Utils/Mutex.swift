import Foundation
import os

/// High-performance thread-safe mutex wrapping `os_unfair_lock`.
/// Provides `.withLock { ... }` semantics identical to ExpoModulesCore Mutex.
final class Mutex<T>: @unchecked Sendable {
  private var unfairLock = os_unfair_lock()
  private var value: T

  init(_ value: T) {
    self.value = value
  }

  func withLock<R>(_ body: (inout T) throws -> R) rethrows -> R {
    os_unfair_lock_lock(&unfairLock)
    defer { os_unfair_lock_unlock(&unfairLock) }
    return try body(&value)
  }
}
