import AVKit
import Foundation

class VideoManager {
  static var shared = VideoManager()

  private static var managerQueue = DispatchQueue(label: "nitro.video.manager.queue")
  private var mediaServicesResetObserver: NSObjectProtocol?
  private var videoViews = NSHashTable<HybridNitroVideoView>.weakObjects()
  private let videoPlayers = SynchronizedHashTable<HybridVideoPlayer>(weakObjects: true)

  var hasRegisteredPlayers: Bool {
    return !videoPlayers.allObjects.isEmpty
  }

  private init() {
    mediaServicesResetObserver = NotificationCenter.default.addObserver(
      forName: AVAudioSession.mediaServicesWereResetNotification,
      object: nil,
      queue: nil
    ) { [weak self] _ in
      self?.onMediaServicesWereReset()
    }
  }

  deinit {
    if let mediaServicesResetObserver {
      NotificationCenter.default.removeObserver(mediaServicesResetObserver)
    }
  }

  func register(videoPlayer: HybridVideoPlayer) {
    videoPlayers.add(videoPlayer)
  }

  func unregister(videoPlayer: HybridVideoPlayer) {
    videoPlayers.remove(videoPlayer)
  }

  func register(videoView: HybridNitroVideoView) {
    videoViews.add(videoView)
  }

  func unregister(videoView: HybridNitroVideoView) {
    videoViews.remove(videoView)
  }

  func onAppForegrounded() {
    reloadPlayers { $0.reloadIfFailed() }
  }

  private func onMediaServicesWereReset() {
    setAppropriateAudioSessionOrWarn()
    reloadPlayers { $0.reloadCurrentSource() }
  }

  private func reloadPlayers(_ reload: @escaping (HybridVideoPlayer) -> Void) {
    Self.managerQueue.async { [weak self] in
      guard let self else {
        return
      }

      let players = self.videoPlayers.allObjects

      DispatchQueue.main.async {
        for player in players {
          reload(player)
        }
      }
    }
  }

  func onAppBackgrounded() {
    for videoView in videoViews.allObjects {
      guard let player = videoView.connectedPlayer else {
        continue
      }
      if player.staysActiveInBackground == true {
        player.avPlayer.audiovisualBackgroundPlaybackPolicy = .continuesIfPossible
      } else if !videoView.playerViewController.isInPictureInPicture {
        player.avPlayer.audiovisualBackgroundPlaybackPolicy = .pauses
        player.avPlayer.pause()
      }
    }
  }

  // MARK: - Audio Session Management

  internal func setAppropriateAudioSessionOrWarn() {
    Self.managerQueue.async { [weak self] in
      self?.setAudioSession()
    }
  }

  private func setAudioSession() {
    let audioSession = AVAudioSession.sharedInstance()
    let audioMixingMode = findAudioMixingMode()
    var audioSessionCategoryOptions: AVAudioSession.CategoryOptions = audioSession.categoryOptions

    let isOutputtingAudio = videoPlayers.allObjects.contains { player in
      player.playing && !player.muted
    }
    let anyPlayerShowsNotification = videoPlayers.allObjects.contains { player in
      player.showNowPlayingNotification
    }

    let shouldMixOverride = audioMixingMode == .mixwithothers
    let doNotMixOverride = audioMixingMode == .donotmix
    let shouldDuckOthers = audioMixingMode == .duckothers && isOutputtingAudio

    let autoShouldMix = !isOutputtingAudio && !anyPlayerShowsNotification
    let shouldMixWithOthers = shouldMixOverride || autoShouldMix

    if shouldMixWithOthers && !shouldDuckOthers && !doNotMixOverride {
      audioSessionCategoryOptions.insert(.mixWithOthers)
    } else {
      audioSessionCategoryOptions.remove(.mixWithOthers)
    }

    if shouldDuckOthers && !doNotMixOverride {
      audioSessionCategoryOptions.insert(.duckOthers)
    } else {
      audioSessionCategoryOptions.remove(.duckOthers)
    }

    if audioSession.categoryOptions != audioSessionCategoryOptions || audioSession.category != .playback || audioSession.mode != .moviePlayback {
      do {
        try audioSession.setCategory(.playback, mode: .moviePlayback, options: audioSessionCategoryOptions)
      } catch {
        print("[nitro-video] Failed to set audio session category: \(error.localizedDescription)")
      }
    }

    if isOutputtingAudio || doNotMixOverride {
      do {
        try audioSession.setActive(true)
      } catch {
        print("[nitro-video] Failed to activate audio session: \(error.localizedDescription)")
      }
    }
  }

  private func findAudioMixingMode() -> AudioMixingMode? {
    let playingPlayers = videoPlayers.allObjects.filter({ player in
      player.playing
    })
    var audioMixingMode: AudioMixingMode = .mixwithothers

    if playingPlayers.isEmpty {
      return nil
    }
    for videoPlayer in playingPlayers where audioMixingMode.priority() < videoPlayer.audioMixingMode.priority() {
      audioMixingMode = videoPlayer.audioMixingMode
    }
    return audioMixingMode
  }
}
