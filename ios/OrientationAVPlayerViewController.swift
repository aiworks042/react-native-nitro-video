import AVKit
import UIKit

internal class OrientationAVPlayerViewController: AVPlayerViewController, AVPlayerViewControllerDelegate {
  weak var forwardDelegate: AVPlayerViewControllerDelegate?
  #if !os(tvOS)
  var fullscreenOrientation: UIInterfaceOrientationMask = UIDevice.current.userInterfaceIdiom == .phone ? .allButUpsideDown : .all
  #endif
  var autoExitOnRotate: Bool = false

  private var hasRotatedToTargetOrientation = false
  var isInPictureInPicture = false
  var keepFullscreenOnPiPStop: KeepFullscreenOnPiPStopBehavior = .never
  private var pipWasAutoEntered = false
  private var wasInFullscreenWhenPiPStarted = false
  private var pendingPiPCompletionHandler: ((Bool) -> Void)?

  var isFullscreen: Bool = false {
    didSet {
      guard oldValue != isFullscreen else {
        return
      }
      if !isFullscreen {
        hasRotatedToTargetOrientation = false
      }
      #if os(tvOS)
      hasRotatedToTargetOrientation = true
      #else
      guard let deviceOrientationMask = UIDevice.current.orientation.toInterfaceOrientationMask(), isFullscreen else {
        return
      }
      hasRotatedToTargetOrientation = fullscreenOrientation.contains(deviceOrientationMask)
      #endif
    }
  }

  #if !os(tvOS)
  override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
    NotificationCenter.default.removeObserver(
      self,
      name: UIDevice.orientationDidChangeNotification,
      object: nil
    )

    if isFullscreen {
      NotificationCenter.default.addObserver(
        self,
        selector: #selector(deviceOrientationDidChange(_:)),
        name: UIDevice.orientationDidChangeNotification,
        object: nil
      )
      return fullscreenOrientation
    }
    return super.supportedInterfaceOrientations
  }
  #endif

  convenience init(delegate: AVPlayerViewControllerDelegate?) {
    self.init()
    self.forwardDelegate = delegate
  }

  deinit {
    pendingPiPCompletionHandler?(false)
    pendingPiPCompletionHandler = nil
    #if !os(tvOS)
    NotificationCenter.default.removeObserver(
      self,
      name: UIDevice.orientationDidChangeNotification,
      object: nil
    )
    #endif
  }

  func enterFullscreen(selectorUnsupportedFallback: (() -> Void)?) {
    let selectorName = "enterFullScreenAnimated:completionHandler:"
    let selectorToEnterFullScreenMode = NSSelectorFromString(selectorName)

    if self.responds(to: selectorToEnterFullScreenMode) {
      self.perform(selectorToEnterFullScreenMode, with: true, with: nil)
    } else {
      selectorUnsupportedFallback?()
    }
  }

  func exitFullscreen() {
    if !isFullscreen {
      return
    }

    let selectorName = "exitFullScreenAnimated:completionHandler:"
    let selectorToExitFullScreenMode = NSSelectorFromString(selectorName)

    if self.responds(to: selectorToExitFullScreenMode) {
      self.perform(selectorToExitFullScreenMode, with: true, with: nil)
    }
  }

  func startPictureInPicture() throws {
    if isInPictureInPicture {
      return
    }
    if !AVPictureInPictureController.isPictureInPictureSupported() {
      throw PictureInPictureUnsupportedException()
    }

    let selectorName = "startPictureInPicture"
    let selectorToStartPictureInPicture = NSSelectorFromString(selectorName)

    if self.responds(to: selectorToStartPictureInPicture) {
      self.perform(selectorToStartPictureInPicture)
    }
  }

  func stopPictureInPicture() {
    if !isInPictureInPicture {
      return
    }
    let selectorName = "stopPictureInPicture"
    let selectorToStopPictureInPicture = NSSelectorFromString(selectorName)

    if self.responds(to: selectorToStopPictureInPicture) {
      self.perform(selectorToStopPictureInPicture)
    }
  }

  override func viewDidLoad() {
    super.viewDidLoad()
    self.delegate = self
  }

  #if !os(tvOS)
  @objc private func deviceOrientationDidChange(_ notification: Notification) {
    guard let deviceOrientationMask = UIDevice.current.orientation.toInterfaceOrientationMask(), isFullscreen else {
      return
    }
    let isPortraitUpsideDownAndUnsupported = UIDevice.current.orientation == .portraitUpsideDown && UIDevice.current.userInterfaceIdiom == .phone
    if isPortraitUpsideDownAndUnsupported {
      return
    }

    hasRotatedToTargetOrientation = fullscreenOrientation.contains(deviceOrientationMask) || hasRotatedToTargetOrientation

    if autoExitOnRotate && !fullscreenOrientation.contains(deviceOrientationMask) && hasRotatedToTargetOrientation {
      self.exitFullscreen()
    }
  }

  // MARK: - AVPlayerViewControllerDelegate
  func playerViewController(
    _ playerViewController: AVPlayerViewController,
    willBeginFullScreenPresentationWithAnimationCoordinator coordinator: any UIViewControllerTransitionCoordinator
  ) {
    forwardDelegate?.playerViewController?(playerViewController, willBeginFullScreenPresentationWithAnimationCoordinator: coordinator)
    coordinator.animate(alongsideTransition: nil) { [weak self] context in
      if context.isCancelled {
        self?.pendingPiPCompletionHandler?(false)
        self?.pendingPiPCompletionHandler = nil
      } else {
        self?.isFullscreen = true
        self?.forceRotationUpdate()
        self?.pendingPiPCompletionHandler?(true)
        self?.pendingPiPCompletionHandler = nil
      }
    }
  }

  func playerViewController(
    _ playerViewController: AVPlayerViewController,
    willEndFullScreenPresentationWithAnimationCoordinator coordinator: any UIViewControllerTransitionCoordinator
  ) {
    forwardDelegate?.playerViewController?(playerViewController, willEndFullScreenPresentationWithAnimationCoordinator: coordinator)
    coordinator.animate(alongsideTransition: nil) { [weak self] context in
      if !context.isCancelled {
        self?.isFullscreen = false
      }
    }
  }
  #endif

  func playerViewControllerDidStartPictureInPicture(_ playerViewController: AVPlayerViewController) {
    isInPictureInPicture = true
    wasInFullscreenWhenPiPStarted = isFullscreen
    pipWasAutoEntered = UIApplication.shared.applicationState != .active
    forwardDelegate?.playerViewControllerDidStartPictureInPicture?(playerViewController)
  }

  func playerViewControllerDidStopPictureInPicture(_ playerViewController: AVPlayerViewController) {
    isInPictureInPicture = false
    pipWasAutoEntered = false
    wasInFullscreenWhenPiPStarted = false
    forwardDelegate?.playerViewControllerDidStopPictureInPicture?(playerViewController)
  }

  func playerViewController(
    _ playerViewController: AVPlayerViewController,
    restoreUserInterfaceForPictureInPictureStopWithCompletionHandler completionHandler: @escaping (Bool) -> Void
  ) {
    if keepFullscreenOnPiPStop.shouldRestore(pipWasAutoEntered: pipWasAutoEntered) && wasInFullscreenWhenPiPStarted {
      pendingPiPCompletionHandler = completionHandler
      self.enterFullscreen(selectorUnsupportedFallback: { [weak self] in
        self?.pendingPiPCompletionHandler?(false)
        self?.pendingPiPCompletionHandler = nil
      })
    } else {
      completionHandler(false)
    }
  }

  #if os(tvOS)
  func playerViewControllerWillBeginDismissalTransition(_ playerViewController: AVPlayerViewController) {
    forwardDelegate?.playerViewControllerWillBeginDismissalTransition?(playerViewController)
  }

  func playerViewControllerDidEndDismissalTransition(_ playerViewController: AVPlayerViewController) {
    forwardDelegate?.playerViewControllerDidEndDismissalTransition?(playerViewController)
  }
  #endif

  #if !os(tvOS)
  private func forceRotationUpdate() {
    if #available(iOS 16.0, *) {
      let windowScene = view.window?.windowScene ?? (UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene) ?? (UIApplication.shared.connectedScenes.first as? UIWindowScene)
      windowScene?.requestGeometryUpdate(.iOS(interfaceOrientations: fullscreenOrientation))
    }
  }
  #endif
}

#if !os(tvOS)
fileprivate extension UIDeviceOrientation {
  func toInterfaceOrientationMask() -> UIInterfaceOrientationMask? {
    switch self {
    case .portrait: return .portrait
    case .portraitUpsideDown: return .portraitUpsideDown
    case .landscapeLeft: return .landscapeLeft
    case .landscapeRight: return .landscapeRight
    case .unknown, .faceUp, .faceDown: return nil
    @unknown default: return nil
    }
  }
}
#endif
