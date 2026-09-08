import AVKit
import Foundation
import React
import UIKit

private let DEFAULT_PRIORITIZE_VIDEO_DEVICES = true

// ─────────────────────────────────────────────────────────────────────────────
// VideoAirPlayButtonView — AVRoutePickerView wrapper for AirPlay device picking
// ─────────────────────────────────────────────────────────────────────────────

@objc(VideoAirPlayButtonView)
public final class VideoAirPlayButtonView: UIView {
  let routePickerView = AVRoutePickerView()
  @objc public var onBeginPresentingRoutes: RCTDirectEventBlock?
  @objc public var onEndPresentingRoutes: RCTDirectEventBlock?

  lazy var delegate = {
    NitroRoutePickerButtonDelegate(
      onWillStartPresentingRoutes: { [weak self] in
        self?.onBeginPresentingRoutes?([:])
      },
      onDidEndPresentingRoutes: { [weak self] in
        self?.onEndPresentingRoutes?([:])
      }
    )
  }()

  @objc public var activeTintColor: UIColor? {
    didSet {
      routePickerView.activeTintColor = activeTintColor
    }
  }

  @objc public var tint: UIColor? {
    didSet {
      routePickerView.tintColor = tint
    }
  }

  @objc public var prioritizeVideoDevices: Bool = DEFAULT_PRIORITIZE_VIDEO_DEVICES {
    didSet {
      routePickerView.prioritizesVideoDevices = prioritizeVideoDevices
    }
  }

  public override init(frame: CGRect) {
    super.init(frame: frame)
    routePickerView.frame = bounds
    routePickerView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    routePickerView.delegate = delegate
    routePickerView.prioritizesVideoDevices = DEFAULT_PRIORITIZE_VIDEO_DEVICES
    addSubview(routePickerView)
  }

  public required init?(coder: NSCoder) {
    super.init(coder: coder)
    routePickerView.frame = bounds
    routePickerView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    routePickerView.delegate = delegate
    routePickerView.prioritizesVideoDevices = DEFAULT_PRIORITIZE_VIDEO_DEVICES
    addSubview(routePickerView)
  }

  public override func layoutSubviews() {
    super.layoutSubviews()
    routePickerView.frame = bounds
  }
}

internal class NitroRoutePickerButtonDelegate: NSObject, AVRoutePickerViewDelegate {
  let onWillStartPresentingRoutes: (() -> Void)?
  let onDidEndPresentingRoutes: (() -> Void)?

  init(onWillStartPresentingRoutes: (() -> Void)?, onDidEndPresentingRoutes: (() -> Void)?) {
    self.onWillStartPresentingRoutes = onWillStartPresentingRoutes
    self.onDidEndPresentingRoutes = onDidEndPresentingRoutes
  }

  func routePickerViewWillBeginPresentingRoutes(_ routePickerView: AVRoutePickerView) {
    onWillStartPresentingRoutes?()
  }

  func routePickerViewDidEndPresentingRoutes(_ routePickerView: AVRoutePickerView) {
    onDidEndPresentingRoutes?()
  }
}
