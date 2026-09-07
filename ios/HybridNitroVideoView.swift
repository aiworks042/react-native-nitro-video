import AVFoundation
import Foundation
import NitroModules
import UIKit

final class NativePlayerUIView: UIView {
  override static var layerClass: AnyClass {
    return AVPlayerLayer.self
  }

  var playerLayer: AVPlayerLayer {
    return layer as! AVPlayerLayer
  }
}

class HybridNitroVideoView: HybridNitroVideoViewSpec {
  let view = NativePlayerUIView()

  private var player: AVPlayer?
  private var timeObserverToken: Any?
  private var statusObserver: NSKeyValueObservation?
  private var itemDidPlayToEndObserver: NSObjectProtocol?
  private var isLoaded = false
  private var lastLoadedSource = ""

  // Properties
  var source: String = "" {
    didSet {
      checkAndLoadSource()
    }
  }

  var paused: Bool? = false {
    didSet {
      updatePlaybackState()
    }
  }

  var muted: Bool? = false {
    didSet {
      player?.isMuted = muted ?? false
    }
  }

  var `repeat`: Bool? = false

  var volume: Double? = 1.0 {
    didSet {
      player?.volume = Float(volume ?? 1.0)
    }
  }

  var resizeMode: ResizeMode? = .contain {
    didSet {
      updateResizeMode()
    }
  }

  var onLoad: ((_ duration: Double) -> Void)?
  var onProgress: ((_ currentTime: Double, _ duration: Double) -> Void)?
  var onEnd: (() -> Void)?
  var onError: ((_ error: String) -> Void)?

  public override init() {
    super.init()
    updateResizeMode()
  }

  deinit {
    cleanUpPlayer()
  }

  func play() throws {
    paused = false
    player?.play()
  }

  func pause() throws {
    paused = true
    player?.pause()
  }

  func seek(position: Double) throws {
    let targetTime = CMTime(seconds: position, preferredTimescale: 600)
    player?.seek(to: targetTime, toleranceBefore: .zero, toleranceAfter: .zero)
  }

  func onDropView() {
    cleanUpPlayer()
  }

  private func cleanUpPlayer() {
    if let token = timeObserverToken {
      player?.removeTimeObserver(token)
      timeObserverToken = nil
    }
    if let observer = itemDidPlayToEndObserver {
      NotificationCenter.default.removeObserver(observer)
      itemDidPlayToEndObserver = nil
    }
    statusObserver?.invalidate()
    statusObserver = nil
    player?.pause()
    player = nil
    view.playerLayer.player = nil
    isLoaded = false
    lastLoadedSource = ""
  }

  private func updateResizeMode() {
    switch resizeMode {
    case .cover:
      view.playerLayer.videoGravity = .resizeAspectFill
    case .contain, .none:
      view.playerLayer.videoGravity = .resizeAspect
    case .stretch:
      view.playerLayer.videoGravity = .resize
    }
  }

  private func updatePlaybackState() {
    if paused == true {
      player?.pause()
    } else {
      player?.play()
    }
  }

  private func checkAndLoadSource() {
    let trimmed = source.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return }
    guard trimmed != lastLoadedSource || !isLoaded else { return }

    cleanUpPlayer()
    lastLoadedSource = trimmed

    guard let url = URL(string: trimmed) else {
      onError?("Invalid video URL: \(trimmed)")
      return
    }

    let playerItem = AVPlayerItem(url: url)
    let newPlayer = AVPlayer(playerItem: playerItem)
    newPlayer.isMuted = muted ?? false
    newPlayer.volume = Float(volume ?? 1.0)
    self.player = newPlayer
    self.containerView.playerLayer.player = newPlayer

    statusObserver = playerItem.observe(\.status, options: [.new]) { [weak self] item, _ in
      guard let self = self else { return }
      DispatchQueue.main.async {
        switch item.status {
        case .readyToPlay:
          if !self.isLoaded {
            self.isLoaded = true
            let durationSeconds = CMTimeGetSeconds(item.duration)
            if !durationSeconds.isNaN && durationSeconds > 0 {
              self.onLoad?(durationSeconds)
            }
          }
          self.updatePlaybackState()
        case .failed:
          let errorDesc = item.error?.localizedDescription ?? "Video playback failed"
          self.onError?(errorDesc)
        default:
          break
        }
      }
    }

    itemDidPlayToEndObserver = NotificationCenter.default.addObserver(
      forName: .AVPlayerItemDidPlayToEndTime,
      object: playerItem,
      queue: .main
    ) { [weak self] _ in
      guard let self = self else { return }
      if self.`repeat` == true {
        self.player?.seek(to: .zero)
        self.player?.play()
      }
      self.onEnd?()
    }

    let interval = CMTime(seconds: 0.25, preferredTimescale: 600)
    timeObserverToken = newPlayer.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
      guard let self = self, let currentItem = self.player?.currentItem else { return }
      let currentTime = CMTimeGetSeconds(time)
      let duration = CMTimeGetSeconds(currentItem.duration)
      if !currentTime.isNaN && !duration.isNaN && duration > 0 {
        self.onProgress?(currentTime, duration)
      }
    }

    updatePlaybackState()
  }
}
