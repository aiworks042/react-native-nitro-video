import AVFoundation
import Foundation

class VideoPlayerSubtitles {
  weak var owner: HybridVideoPlayer?
  private(set) var availableSubtitleTracks: [SubtitleTrack] = []
  private(set) var currentSubtitleTrack: SubtitleTrack?

  init(owner: HybridVideoPlayer) {
    self.owner = owner
  }

  func onNewSubtitleTrackSelected(subtitleTrack: SubtitleTrack?) {
    currentSubtitleTrack = subtitleTrack
  }

  func onNewPlayerItemLoaded(playerItem: AVPlayerItem?) async -> SubtitleTracksChangedEventPayload {
    let oldAvailableSubtitleTracks = availableSubtitleTracks

    do {
      availableSubtitleTracks = try await Self.findAvailableSubtitleTracks(for: playerItem)
    } catch {
      print("[nitro-video] Failed to load available subtitle tracks: \(error)")
      availableSubtitleTracks = []
    }

    return SubtitleTracksChangedEventPayload(
      availableSubtitleTracks: availableSubtitleTracks,
      oldAvailableSubtitleTracks: oldAvailableSubtitleTracks
    )
  }

  func selectSubtitleTrack(subtitleTrack: SubtitleTrack?) {
    guard let currentItem = self.owner?.avPlayer.currentItem else {
      return
    }

    if let group = currentItem.asset.mediaSelectionGroup(forMediaCharacteristic: .legible) {
      let option = group.options.first {
        $0.displayName == subtitleTrack?.label && $0.locale?.identifier == subtitleTrack?.language
      }
      currentItem.select(option, in: group)
    }
  }

  private static func findAvailableSubtitleTracks(for playerItem: AVPlayerItem?) async throws -> [SubtitleTrack] {
    var availableSubtitleTracks: [SubtitleTrack] = []

    guard let asset = await playerItem?.asset else {
      return availableSubtitleTracks
    }
    let mediaSelectionCharacteristics = try await asset.load(.availableMediaCharacteristicsWithMediaSelectionOptions)

    for characteristic in mediaSelectionCharacteristics {
      guard characteristic == .legible else {
        continue
      }

      if let group = try await asset.loadMediaSelectionGroup(for: characteristic) {
        for option in group.options {
          guard let subtitleTrack = SubtitleTrack.from(mediaSelectionOption: option, in: group) else {
            continue
          }
          availableSubtitleTracks.append(subtitleTrack)
        }
      }
    }
    return availableSubtitleTracks
  }

  static func findCurrentSubtitleTrack(for playerItem: AVPlayerItem?) async -> SubtitleTrack? {
    guard
      let currentItem = playerItem,
      let mediaSelectionGroup = try? await currentItem.asset.loadMediaSelectionGroup(for: .legible),
      let selectedMediaOption = await currentItem.currentMediaSelection.selectedMediaOption(in: mediaSelectionGroup)
    else {
      return nil
    }

    return SubtitleTrack.from(mediaSelectionOption: selectedMediaOption, in: mediaSelectionGroup)
  }
}
