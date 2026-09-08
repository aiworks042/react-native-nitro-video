import Foundation

internal func runOnMainThread(_ operation: @escaping () -> Void) {
  if Thread.isMainThread {
    operation()
  } else {
    DispatchQueue.main.async(execute: operation)
  }
}
