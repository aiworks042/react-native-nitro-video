import Foundation
import React
import UIKit

// ─────────────────────────────────────────────────────────────────────────────
// NitroVideoViewManager — RCTViewManager for NitroVideoView in React Native CLI
// ─────────────────────────────────────────────────────────────────────────────

@objc(NitroVideoViewManager)
public final class NitroVideoViewManager: RCTViewManager {

  public override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  public override func view() -> UIView! {
    return NitroVideoView()
  }

  @objc func enterFullscreen(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? NitroVideoView else { return }
      view.enterFullscreen()
    }
  }

  @objc func exitFullscreen(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? NitroVideoView else { return }
      view.exitFullscreen()
    }
  }

  @objc func startPictureInPicture(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? NitroVideoView else { return }
      try? view.startPictureInPicture()
    }
  }

  @objc func stopPictureInPicture(_ reactTag: NSNumber) {
    bridge.uiManager.addUIBlock { _, viewRegistry in
      guard let view = viewRegistry?[reactTag] as? NitroVideoView else { return }
      view.stopPictureInPicture()
    }
  }
}
