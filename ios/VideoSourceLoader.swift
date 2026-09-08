import AVKit
import Foundation

private struct State {
  var isLoading = true
  var currentSource: VideoSource?
  var currentTask: Task<LoadingResult, Error>?
  var currentTaskId = 0
  var isClosed = false
  var listeners = Set<WeakVideoSourceLoaderListener>()
}

private struct LoadContext {
  let task: Task<LoadingResult, Error>
  let taskId: Int
}

private struct PreviousLoad {
  let task: Task<LoadingResult, Error>
  let source: VideoSource?
}

internal class VideoSourceLoader {
  private let state = Mutex(State())

  var isLoading: Bool {
    return state.withLock { $0.isLoading }
  }

  func registerListener(listener: VideoSourceLoaderListener) {
    let weakListener = WeakVideoSourceLoaderListener(value: listener)
    state.withLock { state in
      state.listeners = state.listeners.filter { $0.value != nil }
      state.listeners.insert(weakListener)
    }
  }

  func unregisterListener(listener: VideoSourceLoaderListener) {
    state.withLock { state in
      state.listeners.remove(WeakVideoSourceLoaderListener(value: listener))
    }
  }

  func load(videoSource: VideoSource) async throws -> VideoPlayerItem? {
    let loadContext: (LoadContext, PreviousLoad?)? = state.withLock { state in
      guard !state.isClosed else {
        return nil
      }

      let previousLoad: PreviousLoad?
      if let currentTask = state.currentTask {
        previousLoad = PreviousLoad(task: currentTask, source: state.currentSource)
      } else {
        previousLoad = nil
      }

      state.currentTaskId += 1
      let taskId = state.currentTaskId
      let newTask = Task {
        return try await self.loadImpl(videoSource: videoSource)
      }

      state.isLoading = true
      state.currentTask = newTask
      state.currentSource = videoSource

      if let previousLoad {
        enqueueListenerEventWhileLocked(listeners: state.listeners) { listener, loader in
          listener.onLoadingCancelled(loader: loader, videoSource: previousLoad.source)
        }
      }
      enqueueListenerEventWhileLocked(listeners: state.listeners) { listener, loader in
        listener.onLoadingStarted(loader: loader, videoSource: videoSource)
      }

      return (LoadContext(task: newTask, taskId: taskId), previousLoad)
    }

    guard let (context, previousLoad) = loadContext else {
      return nil
    }

    previousLoad?.task.cancel()

    let loadingResult: LoadingResult
    do {
      loadingResult = try await context.task.value
    } catch {
      finishLoading(taskId: context.taskId, videoSource: videoSource)
      throw error
    }

    guard finishLoading(
      taskId: context.taskId,
      videoSource: videoSource,
      loadingResult: loadingResult
    ) else {
      return nil
    }

    return loadingResult.value
  }

  func cancelCurrentTask() {
    cancelCurrentTask(close: false)
  }

  func close() {
    cancelCurrentTask(close: true)
  }

  deinit {
    cancelCurrentTask()
  }

  @VideoLoadingActor
  private func loadImpl(videoSource: VideoSource) async throws -> LoadingResult {
    do {
      try Task.checkCancellation()

      guard let url = videoSource.uri else {
        return LoadingResult(value: nil, isCancelled: false)
      }

      var safeUrl: URL?
      var resolutionError: Error?
      do {
        safeUrl = try await url.toUrlWithPermissions()
      } catch let error as CancellationError {
        throw error
      } catch {
        resolutionError = error
      }

      let playerItem = try await VideoPlayerItem(videoSource: videoSource, urlOverride: safeUrl)
      if let resolutionError {
        playerItem?.urlAsset.transportError = resolutionError
      }

      try Task.checkCancellation()
      return LoadingResult(value: playerItem, isCancelled: false)
    } catch is CancellationError {
      return LoadingResult(value: nil, isCancelled: true)
    }
  }

  @discardableResult
  private func finishLoading(
    taskId: Int,
    videoSource: VideoSource? = nil,
    loadingResult: LoadingResult? = nil
  ) -> Bool {
    return state.withLock { state in
      guard state.currentTaskId == taskId else {
        return false
      }
      state.isLoading = false
      state.currentSource = nil
      state.currentTask = nil

      if let videoSource, let loadingResult, !loadingResult.isCancelled {
        enqueueListenerEventWhileLocked(listeners: state.listeners) { listener, loader in
          listener.onLoadingFinished(loader: loader, videoSource: videoSource, result: loadingResult.value)
        }
      } else {
        enqueueListenerEventWhileLocked(listeners: state.listeners) { listener, loader in
          listener.onLoadingCancelled(loader: loader, videoSource: videoSource)
        }
      }
      return true
    }
  }

  private func cancelCurrentTask(close: Bool) {
    let currentTask = state.withLock { state in
      state.currentTaskId += 1
      state.isClosed = state.isClosed || close
      state.isLoading = false
      let currentSource = state.currentSource
      state.currentSource = nil
      let currentTask = state.currentTask
      state.currentTask = nil

      if currentTask != nil {
        enqueueListenerEventWhileLocked(listeners: state.listeners) { listener, loader in
          listener.onLoadingCancelled(loader: loader, videoSource: currentSource)
        }
      }
      return currentTask
    }
    currentTask?.cancel()
  }

  private func enqueueListenerEventWhileLocked(
    listeners: Set<WeakVideoSourceLoaderListener>,
    event: @escaping (VideoSourceLoaderListener, VideoSourceLoader) -> Void
  ) {
    DispatchQueue.main.async { [weak self] in
      guard let self else {
        return
      }
      let currentListeners = self.state.withLock { $0.listeners }
      for weakListener in listeners.intersection(currentListeners) {
        guard let listener = weakListener.value else {
          continue
        }
        event(listener, self)
      }
    }
  }
}

private struct LoadingResult {
  let value: VideoPlayerItem?
  let isCancelled: Bool
}
