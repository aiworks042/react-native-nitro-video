import AVKit
import Foundation

class VideoPlayerItem: AVPlayerItem {
  let urlAsset: VideoAsset
  let videoSource: VideoSource
  let isHls: Bool
  var videoTracks: [VideoTrack] {
    get async {
      return await tracksLoadingTask?.value ?? []
    }
  }

  private var tracksLoadingTask: Task<[VideoTrack], Never>?

  init?(videoSource: VideoSource) {
    guard let url = videoSource.uri else {
      return nil
    }
    self.videoSource = videoSource

    let asset = VideoAsset(url: url, videoSource: videoSource)
    self.urlAsset = asset
    self.isHls = asset.effectivePlaybackURL.isHLS || asset.effectiveContentType == .hls
    super.init(asset: urlAsset, automaticallyLoadedAssetKeys: nil)
    self.createTracksLoadingTask()
  }

  @VideoLoadingActor
  init?(videoSource: VideoSource, urlOverride: URL? = nil) async throws {
    try Task.checkCancellation()

    guard let url = urlOverride ?? videoSource.uri else {
      return nil
    }
    self.videoSource = videoSource

    let asset = VideoAsset(url: url, videoSource: videoSource)
    self.urlAsset = asset
    self.isHls = asset.effectivePlaybackURL.isHLS || asset.effectiveContentType == .hls

    do {
      try await asset.prepareForLoadingIfNeeded()
      let (_, _, _, _, _, characteristics) = try await asset.load(
        .duration,
        .preferredTransform,
        .isPlayable,
        .hasProtectedContent,
        .tracks,
        .availableMediaCharacteristicsWithMediaSelectionOptions
      )
      for characteristic in characteristics {
        _ = try await asset.loadMediaSelectionGroup(for: characteristic)
      }
    } catch {
      try Task.checkCancellation()
    }

    try Task.checkCancellation()
    super.init(asset: urlAsset, automaticallyLoadedAssetKeys: nil)
    try Task.checkCancellation()
    self.createTracksLoadingTask()
  }

  deinit {
    tracksLoadingTask?.cancel()
  }

  func createTracksLoadingTask() {
    tracksLoadingTask = Task { @VideoLoadingActor [weak self] in
      guard let self else {
        return []
      }
      let mainUrl = urlAsset.effectivePlaybackURL

      var tracks: [VideoTrack] = []
      if let assetTracks = try? await urlAsset.loadTracks(withMediaType: .video) {
        for avAssetTrack in assetTracks {
          tracks.append(await VideoTrack.from(assetTrack: avAssetTrack))
        }
      }

      guard isHls else {
        return tracks
      }

      let hlsTracks = await loadHlsTracks(mainUrl: mainUrl)
      return tracks + hlsTracks
    }
  }

  // MARK: - HLS Helpers

  @VideoLoadingActor
  private func loadHlsTracks(mainUrl: URL) async -> [VideoTrack] {
    let tracks: [VideoTrack]

    if #available(iOS 16.0, tvOS 16.0, *) {
      tracks = await loadModernHlsTracks(mainUrl: mainUrl)
    } else {
      tracks = await loadLegacyHlsTracks()
    }

    var seen: [VideoTrack] = []
    for track in tracks where !seen.contains(track) {
      seen.append(track)
    }

    return seen
  }

  @available(iOS 16.0, tvOS 16.0, *)
  @VideoLoadingActor
  private func loadModernHlsTracks(mainUrl: URL) async -> [VideoTrack] {
    guard let variants = try? await urlAsset.load(.variants) else {
      return []
    }
    let isPlayable = (try? await urlAsset.load(.isPlayable)) ?? false

    return variants.compactMap { variant in
      VideoTrack.from(assetVariant: variant, isPlayable: isPlayable, mainUrl: mainUrl)
    }
  }

  @VideoLoadingActor
  private func loadLegacyHlsTracks() async -> [VideoTrack] {
    do {
      return try await self.fetchHlsVideoTracks()
    } catch {
      print("[nitro-video] Failed to fetch HLS video tracks: \(error.localizedDescription)")
      return []
    }
  }

  @VideoLoadingActor
  private func fetchHlsVideoTracks() async throws -> [VideoTrack] {
    let uri = urlAsset.effectivePlaybackURL
    var request = URLRequest(url: uri)
    if let headers = videoSource.headers {
      for (key, value) in headers {
        request.addValue(value, forHTTPHeaderField: key)
      }
    }

    let (data, _) = try await URLSession.shared.data(for: request)
    let content = String(data: data, encoding: .utf8) ?? ""
    return parseM3U8(content, mainUrl: uri)
  }

  private func parseM3U8(_ content: String, mainUrl: URL) -> [VideoTrack] {
    let lines = content.components(separatedBy: "\n")
    return zip(lines, lines.dropFirst()).compactMap { line, nextLine in
      VideoTrack.from(hlsHeaderLine: line, idLine: nextLine, mainUrl: mainUrl)
    }
  }
}

private extension URL {
  var isHLS: Bool {
    return self.pathExtension.lowercased() == "m3u8" || self.absoluteString.lowercased().hasSuffix("m3u8")
  }
}
