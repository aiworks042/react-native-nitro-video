import AVKit
import Foundation

/**
 * Seeks the player to provided time. When scrubbingModeEnabled is enabled, the seeker will only accept seek requests once the previous seek has completed resulting
 * in less seek interruptions and more displayed frames. See: https://developer.apple.com/library/archive/qa/qa1820/_index.html#//apple_ref/doc/uid/DTS40016828.
 */
class VideoPlayerSeeker {
  weak var player: AVPlayer?
  var seekTolerance: SeekTolerance
  var scrubbingModeOptions = ScrubbingModeOptions()

  private var isWaitingForOptimizedSeek = false
  private var chaseTime = CMTime.zero

  init(player: AVPlayer?, seekTolerance: SeekTolerance = SeekTolerance()) {
    self.player = player
    self.seekTolerance = seekTolerance
  }

  func seek(to time: CMTime) {
    if scrubbingModeOptions.scrubbingModeEnabled || isWaitingForOptimizedSeek {
      updateChaseTime(to: time)
    } else {
      player?.seek(
        to: time,
        toleranceBefore: seekTolerance.cmTimeToleranceBefore,
        toleranceAfter: seekTolerance.cmTimeToleranceAfter
      )
    }
  }

  private func updateChaseTime(to newTime: CMTime) {
    guard newTime != chaseTime else {
      return
    }

    chaseTime = newTime

    if !isWaitingForOptimizedSeek {
      seekToChaseTime()
    }
  }

  private func seekToChaseTime() {
    guard let player = player else {
      return
    }

    isWaitingForOptimizedSeek = true
    let seekTimeInProgress = chaseTime

    player.seek(to: seekTimeInProgress, seekTolerance: seekTolerance) { [weak self] _ in
      guard let self else {
        return
      }

      if seekTimeInProgress == self.chaseTime {
        self.isWaitingForOptimizedSeek = false
      } else {
        self.seekToChaseTime()
      }
    }
  }
}

internal extension AVPlayer {
  func seek(to time: CMTime, seekTolerance: SeekTolerance, completionHandler: @escaping (Bool) -> Void = { _ in }) {
    self.seek(
      to: time,
      toleranceBefore: seekTolerance.cmTimeToleranceBefore,
      toleranceAfter: seekTolerance.cmTimeToleranceAfter,
      completionHandler: completionHandler
    )
  }
}
