import AVKit
import Foundation

public struct SubtitleTrack: Codable, Equatable, Sendable {
  public var id: String?
  public var language: String?
  public var label: String?
  public var name: String?
  public var isDefault: Bool
  public var autoSelect: Bool

  public init(
    id: String? = nil,
    language: String? = nil,
    label: String? = nil,
    name: String? = nil,
    isDefault: Bool = false,
    autoSelect: Bool = false
  ) {
    self.id = id ?? language ?? label ?? "unknown"
    self.language = language
    self.label = label
    self.name = name
    self.isDefault = isDefault
    self.autoSelect = autoSelect
  }

  public static func from(mediaSelectionOption option: AVMediaSelectionOption, in group: AVMediaSelectionGroup? = nil) -> SubtitleTrack? {
    guard let identifier = option.locale?.identifier else {
      return nil
    }

    let isDefault = group?.defaultOption == option
    let autoSelect = option.hasMediaCharacteristic(.isAuxiliaryContent) == false

    return SubtitleTrack(
      id: identifier,
      language: identifier,
      label: option.displayName,
      name: option.commonMetadata.first(where: { $0.commonKey == .commonKeyTitle })?.stringValue,
      isDefault: isDefault,
      autoSelect: autoSelect
    )
  }
}

public struct AudioTrack: Codable, Equatable, Sendable {
  public var id: String?
  public var language: String?
  public var label: String?
  public var name: String?
  public var isDefault: Bool
  public var autoSelect: Bool

  public init(
    id: String? = nil,
    language: String? = nil,
    label: String? = nil,
    name: String? = nil,
    isDefault: Bool = false,
    autoSelect: Bool = false
  ) {
    self.id = id ?? language ?? label ?? "unknown"
    self.language = language
    self.label = label
    self.name = name
    self.isDefault = isDefault
    self.autoSelect = autoSelect
  }

  public static func from(mediaSelectionOption option: AVMediaSelectionOption, in group: AVMediaSelectionGroup? = nil) -> AudioTrack? {
    guard let identifier = option.locale?.identifier else {
      return nil
    }

    let isDefault = group?.defaultOption == option
    let autoSelect = option.hasMediaCharacteristic(.isAuxiliaryContent) == false

    return AudioTrack(
      id: identifier,
      language: identifier,
      label: option.displayName,
      name: option.commonMetadata.first(where: { $0.commonKey == .commonKeyTitle })?.stringValue,
      isDefault: isDefault,
      autoSelect: autoSelect
    )
  }
}

public struct VideoTrack: Codable, Equatable, Sendable {
  public var id: String?
  public var url: URL?
  public var size: VideoSize?
  public var mimeType: String?
  public var bitrate: Int?
  public var peakBitrate: Int?
  public var averageBitrate: Int?
  public var isSupported: Bool
  public var frameRate: Float?
  public var videoRange: VideoRange?

  public init(
    id: String? = nil,
    url: URL? = nil,
    size: VideoSize? = nil,
    mimeType: String? = nil,
    bitrate: Int? = nil,
    peakBitrate: Int? = nil,
    averageBitrate: Int? = nil,
    isSupported: Bool = true,
    frameRate: Float? = nil,
    videoRange: VideoRange? = nil
  ) {
    self.id = id
    self.url = url
    self.size = size
    self.mimeType = mimeType
    self.bitrate = bitrate
    self.peakBitrate = peakBitrate
    self.averageBitrate = averageBitrate
    self.isSupported = isSupported
    self.frameRate = frameRate
    self.videoRange = videoRange
  }

  public static func == (lhs: VideoTrack, rhs: VideoTrack) -> Bool {
    guard lhs.id != nil, rhs.id != nil else {
      return false
    }
    return lhs.id == rhs.id
  }

  public static func from(assetTrack: AVAssetTrack) async -> VideoTrack {
    var averageBitrate: Int?
    var size: VideoSize?
    let supported = (try? await assetTrack.load(.isPlayable)) ?? true
    let mediaFormat = try? await assetTrack.mediaFormat
    let frameRate = try? await assetTrack.load(.nominalFrameRate)
    let safeFrameRate = (frameRate?.isFinite == true) ? frameRate : nil

    if let bitrateFloat = try? await assetTrack.load(.estimatedDataRate), bitrateFloat.isFinite {
      averageBitrate = Int(bitrateFloat)
    }

    let videoRange = VideoRange.from(videoRange: await assetTrack.avVideoRange)
    let peakBitrate = await assetTrack.getPeakBitrate()

    if let cgSize = try? await assetTrack.load(.naturalSize), cgSize.width.isFinite, cgSize.height.isFinite {
      size = VideoSize.from(cgSize)
    }

    return VideoTrack(
      id: "\(assetTrack.trackID)",
      size: size,
      mimeType: mediaFormat,
      bitrate: peakBitrate ?? averageBitrate,
      peakBitrate: peakBitrate,
      averageBitrate: averageBitrate,
      isSupported: supported,
      frameRate: safeFrameRate,
      videoRange: videoRange
    )
  }

  @available(iOS 16, tvOS 16, *)
  public static func from(assetVariant: AVAssetVariant, isPlayable: Bool, mainUrl: URL) -> VideoTrack? {
    guard let videoAttributes = assetVariant.videoAttributes else {
      return nil
    }

    let trackUrl = assetVariant.url
    let id = extractHlsTrackId(trackUrl: trackUrl, mainUrl: mainUrl)
    let videoSize = videoAttributes.videoSize
    let mimeType = videoAttributes.getFormattedCodecString()
    let frameRate = videoAttributes.nominalFrameRate.flatMap(Float.init)
    let peakBitrate = assetVariant.peakBitRate.flatMap { $0.isFinite ? Int($0) : nil }
    let averageBitrate = assetVariant.averageBitRate.flatMap { $0.isFinite ? Int($0) : nil }
    let safeFrameRate = (frameRate?.isFinite == true) ? frameRate : nil
    let videoRange = VideoRange.from(videoRange: assetVariant.videoAttributes?.videoRange.flatMap {
      switch $0 {
      case .sdr: return .sdr
      case .hlg: return .hlg
      case .pq: return .pq
      @unknown default: return .sdr
      }
    } ?? .sdr)

    return VideoTrack(
      id: id,
      url: trackUrl,
      size: videoSize,
      mimeType: mimeType,
      bitrate: peakBitrate ?? averageBitrate,
      peakBitrate: peakBitrate,
      averageBitrate: averageBitrate,
      isSupported: isPlayable,
      frameRate: safeFrameRate,
      videoRange: videoRange
    )
  }

  public static func from(hlsHeaderLine: String, idLine: String, mainUrl: URL) -> VideoTrack? {
    guard hlsHeaderLine.starts(with: "#EXT-X-STREAM-INF"), hlsHeaderLine.contains("RESOLUTION") else {
      return nil
    }

    let details = hlsHeaderLine.split(separator: ",")
      .reduce(into: [String: String]()) { dict, detail in
        let pair = detail.split(separator: "=", maxSplits: 1).map {
          String($0).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if pair.count == 2 {
          let (key, value) = (pair[0], pair[1])
          dict[key] = value.trimmingCharacters(in: CharacterSet(charactersIn: "\""))
        }
      }
    guard let resolution = details["RESOLUTION"] else {
      return nil
    }

    let dimensions = resolution.split(separator: "x").map { Int($0) }
    guard dimensions.count == 2, let width = dimensions[0], let height = dimensions[1] else {
      return nil
    }

    let id = idLine.trimmingCharacters(in: .whitespacesAndNewlines)
    let size = VideoSize(width: width, height: height)
    let mimeType = codecsToMimeType(codecs: details["CODECS"])
    let videoRangeString = details["VIDEO-RANGE"]?.lowercased() ?? "sdr"
    let videoRange = VideoRange(rawValue: videoRangeString) ?? .sdr
    var peakBitrate: Int? = nil
    var averageBitrate: Int? = nil
    var frameRate: Float? = nil

    if let peakBitrateString = details["BANDWIDTH"] {
      peakBitrate = Int(peakBitrateString)
    }
    if let averageBitrateString = details["AVERAGE-BANDWIDTH"] {
      averageBitrate = Int(averageBitrateString)
    }
    if let frameRateString = details["FRAME-RATE"] {
      frameRate = Float(frameRateString)
    }

    let bitrate = peakBitrate ?? averageBitrate

    return VideoTrack(
      id: id,
      url: resolveMediaUrl(pathLine: idLine, mainUrl: mainUrl),
      size: size,
      mimeType: mimeType,
      bitrate: bitrate,
      peakBitrate: peakBitrate,
      averageBitrate: averageBitrate,
      isSupported: true,
      frameRate: frameRate,
      videoRange: videoRange
    )
  }

  private static func codecsToMimeType(codecs: String?) -> String? {
    guard let codecs else {
      return nil
    }
    if codecs.starts(with: "avc1") {
      return "video/avc"
    }
    if codecs.starts(with: "hvc1") {
      return "video/hevc"
    }
    if codecs.starts(with: "dvh1") {
      return "video/dolby-vision"
    }
    if codecs.starts(with: "av11") {
      return "video/av1"
    }
    return nil
  }
}
