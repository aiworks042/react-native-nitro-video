import AVKit
import Foundation
import React
import UIKit

// ─────────────────────────────────────────────────────────────────────────────
// NitroVideoView — Native UIView for React Native CLI requireNativeComponent
// Integrates with OrientationAVPlayerViewControllerWrapper.
// ─────────────────────────────────────────────────────────────────────────────

@objc(NitroVideoView)
public final class NitroVideoView: UIView, AVPlayerViewControllerDelegate {

  private lazy var playerViewControllerWrapper = OrientationAVPlayerViewControllerWrapper(delegate: self)
  var playerViewController: OrientationAVPlayerViewController {
    playerViewControllerWrapper.controller
  }

  weak var player: HybridVideoPlayer? {
    didSet {
      if playerViewControllerWrapper.setPlayer(player) {
        removeFirstFrameObserver()
        addFirstFrameObserver()
      }
    }
  }

  @objc public var playerId: NSNumber? {
    didSet {
      guard let id = playerId?.intValue,
            let found = NitroVideoPlayerRegistry.shared.player(forId: id) else {
        player = nil
        return
      }
      player = found
    }
  }

  @objc public var nativeControls: Bool = true {
    didSet {
      playerViewController.showsPlaybackControls = nativeControls
    }
  }

  @objc public var contentFit: String = "contain" {
    didSet {
      let fit = VideoContentFit(fromString: contentFit) ?? .contain
      playerViewController.videoGravity = fit.toVideoGravity()
    }
  }

  @objc public var allowsPictureInPicture: Bool = false {
    didSet {
      VideoManager.shared.setAppropriateAudioSessionOrWarn()
      playerViewController.allowsPictureInPicturePlayback = allowsPictureInPicture
    }
  }

  @objc public var startsPictureInPictureAutomatically: Bool = false {
    didSet {
      #if !os(tvOS)
      playerViewController.canStartPictureInPictureAutomaticallyFromInline = startsPictureInPictureAutomatically
      #endif
    }
  }

  @objc public var requiresLinearPlayback: Bool = false {
    didSet {
      playerViewController.requiresLinearPlayback = requiresLinearPlayback
    }
  }

  @objc public var useExoShutter: Bool = false
  @objc public var controllerAutoShow: Bool = true

  @objc public var onPictureInPictureStart: RCTDirectEventBlock?
  @objc public var onPictureInPictureStop: RCTDirectEventBlock?
  @objc public var onFullscreenEnter: RCTDirectEventBlock?
  @objc public var onFullscreenExit: RCTDirectEventBlock?
  @objc public var onFirstFrameRender: RCTDirectEventBlock?

  private var firstFrameObserver: NSKeyValueObservation?

  public override var bounds: CGRect {
    didSet {
      playerViewControllerWrapper.view.frame = bounds
      playerViewControllerWrapper.layoutControllerView()
    }
  }

  public override init(frame: CGRect) {
    super.init(frame: frame)
    clipsToBounds = true
    addSubview(playerViewControllerWrapper.view)
    playerViewController.showsPlaybackControls = nativeControls
    addFirstFrameObserver()
  }

  public required init?(coder: NSCoder) {
    super.init(coder: coder)
    clipsToBounds = true
    addSubview(playerViewControllerWrapper.view)
    playerViewController.showsPlaybackControls = nativeControls
    addFirstFrameObserver()
  }

  deinit {
    removeFirstFrameObserver()
    player = nil
  }

  public override func layoutSubviews() {
    super.layoutSubviews()
    playerViewControllerWrapper.view.frame = bounds
    playerViewControllerWrapper.layoutControllerView()
  }

  func enterFullscreen() {
    playerViewController.enterFullscreen(selectorUnsupportedFallback: nil)
  }

  func exitFullscreen() {
    playerViewController.exitFullscreen()
  }

  func startPictureInPicture() throws {
    try playerViewController.startPictureInPicture()
  }

  func stopPictureInPicture() {
    playerViewController.stopPictureInPicture()
  }

  // MARK: - AVPlayerViewControllerDelegate

  #if !os(tvOS)
  public func playerViewController(
    _ playerViewController: AVPlayerViewController,
    willBeginFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator
  ) {
    onFullscreenEnter?([:])
  }

  public func playerViewController(
    _ playerViewController: AVPlayerViewController,
    willEndFullScreenPresentationWithAnimationCoordinator coordinator: UIViewControllerTransitionCoordinator
  ) {
    let wasPlaying = player?.playing ?? false
    coordinator.animate(alongsideTransition: nil) { [weak self] context in
      if !context.isCancelled && wasPlaying {
        DispatchQueue.main.async {
          try? self?.player?.play()
        }
      }
      if !context.isCancelled {
        self?.onFullscreenExit?([:])
      }
    }
  }
  #endif

  public func playerViewControllerDidStartPictureInPicture(_ playerViewController: AVPlayerViewController) {
    onPictureInPictureStart?([:])
  }

  public func playerViewControllerDidStopPictureInPicture(_ playerViewController: AVPlayerViewController) {
    onPictureInPictureStop?([:])
  }

  private func addFirstFrameObserver() {
    removeFirstFrameObserver()
    firstFrameObserver = playerViewController.observe(\.isReadyForDisplay) { [weak self] controller, _ in
      if controller.isReadyForDisplay {
        DispatchQueue.main.async {
          self?.onFirstFrameRender?([:])
        }
      }
    }
  }

  private func removeFirstFrameObserver() {
    firstFrameObserver?.invalidate()
    firstFrameObserver = nil
  }
}
