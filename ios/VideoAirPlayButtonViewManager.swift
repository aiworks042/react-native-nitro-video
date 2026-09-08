import Foundation
import React
import UIKit

// ─────────────────────────────────────────────────────────────────────────────
// VideoAirPlayButtonViewManager — RCTViewManager for AirPlay button
// ─────────────────────────────────────────────────────────────────────────────

@objc(VideoAirPlayButtonViewManager)
public final class VideoAirPlayButtonViewManager: RCTViewManager {

  public override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  public override func view() -> UIView! {
    return VideoAirPlayButtonView()
  }
}
