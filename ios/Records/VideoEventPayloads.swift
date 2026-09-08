import Foundation

public struct StatusChangedEventPayload: Codable, Sendable {
  public var status: String
  public var oldStatus: String?
  public var error: PlaybackError?

  public init(status: VideoPlayerStatus, oldStatus: VideoPlayerStatus? = nil, error: PlaybackError? = nil) {
    self.status = status.stringValue
    self.oldStatus = oldStatus?.stringValue
    self.error = error
  }
}

public struct IsPlayingEventPayload: Codable, Sendable {
  public var isPlaying: Bool
  public var oldIsPlaying: Bool?

  public init(isPlaying: Bool, oldIsPlaying: Bool? = nil) {
    self.isPlaying = isPlaying
    self.oldIsPlaying = oldIsPlaying
  }
}

public struct VolumeChangedEventPayload: Codable, Sendable {
  public var volume: Float
  public var oldVolume: Float?

  public init(volume: Float, oldVolume: Float? = nil) {
    self.volume = volume
    self.oldVolume = oldVolume
  }
}

public struct MutedChangedEventPayload: Codable, Sendable {
  public var muted: Bool
  public var oldMuted: Bool?

  public init(muted: Bool, oldMuted: Bool? = nil) {
    self.muted = muted
    self.oldMuted = oldMuted
  }
}

public struct SourceChangedEventPayload: Codable, Sendable {
  public var source: VideoSource?
  public var oldSource: VideoSource?

  public init(source: VideoSource? = nil, oldSource: VideoSource? = nil) {
    self.source = source
    self.oldSource = oldSource
  }
}

public struct PlaybackRateChangedEventPayload: Codable, Sendable {
  public var playbackRate: Float
  public var oldPlaybackRate: Float?

  public init(playbackRate: Float, oldPlaybackRate: Float? = nil) {
    self.playbackRate = playbackRate
    self.oldPlaybackRate = oldPlaybackRate
  }
}

public struct SubtitleTracksChangedEventPayload: Codable, Sendable {
  public var availableSubtitleTracks: [SubtitleTrack]
  public var oldAvailableSubtitleTracks: [SubtitleTrack]

  public init(availableSubtitleTracks: [SubtitleTrack] = [], oldAvailableSubtitleTracks: [SubtitleTrack] = []) {
    self.availableSubtitleTracks = availableSubtitleTracks
    self.oldAvailableSubtitleTracks = oldAvailableSubtitleTracks
  }
}

public struct SubtitleTrackChangedEventPayload: Codable, Sendable {
  public var subtitleTrack: SubtitleTrack?
  public var oldSubtitleTrack: SubtitleTrack?

  public init(subtitleTrack: SubtitleTrack? = nil, oldSubtitleTrack: SubtitleTrack? = nil) {
    self.subtitleTrack = subtitleTrack
    self.oldSubtitleTrack = oldSubtitleTrack
  }
}

public struct AudioTracksChangedEventPayload: Codable, Sendable {
  public var availableAudioTracks: [AudioTrack]
  public var oldAvailableAudioTracks: [AudioTrack]

  public init(availableAudioTracks: [AudioTrack] = [], oldAvailableAudioTracks: [AudioTrack] = []) {
    self.availableAudioTracks = availableAudioTracks
    self.oldAvailableAudioTracks = oldAvailableAudioTracks
  }
}

public struct AudioTrackChangedEventPayload: Codable, Sendable {
  public var audioTrack: AudioTrack?
  public var oldAudioTrack: AudioTrack?

  public init(audioTrack: AudioTrack? = nil, oldAudioTrack: AudioTrack? = nil) {
    self.audioTrack = audioTrack
    self.oldAudioTrack = oldAudioTrack
  }
}

public struct TimeUpdate: Codable, Sendable {
  public var currentTime: Double
  public var currentLiveTimestamp: Double?
  public var currentOffsetFromLive: Double?
  public var bufferedPosition: Double

  public init(
    currentTime: Double = 0,
    currentLiveTimestamp: Double? = nil,
    currentOffsetFromLive: Double? = nil,
    bufferedPosition: Double = -1
  ) {
    self.currentTime = currentTime
    self.currentLiveTimestamp = currentLiveTimestamp
    self.currentOffsetFromLive = currentOffsetFromLive
    self.bufferedPosition = bufferedPosition
  }
}

public struct VideoTrackChangedEventPayload: Codable, Sendable {
  public var videoTrack: VideoTrack?
  public var oldVideoTrack: VideoTrack?

  public init(videoTrack: VideoTrack? = nil, oldVideoTrack: VideoTrack? = nil) {
    self.videoTrack = videoTrack
    self.oldVideoTrack = oldVideoTrack
  }
}

public struct VideoSourceLoadedEventPayload: Codable, Sendable {
  public var videoSource: VideoSource?
  public var duration: Double?
  public var availableVideoTracks: [VideoTrack]?
  public var availableSubtitleTracks: [SubtitleTrack]?
  public var availableAudioTracks: [AudioTrack]?

  public init(
    videoSource: VideoSource? = nil,
    duration: Double? = nil,
    availableVideoTracks: [VideoTrack]? = nil,
    availableSubtitleTracks: [SubtitleTrack]? = nil,
    availableAudioTracks: [AudioTrack]? = nil
  ) {
    self.videoSource = videoSource
    self.duration = duration
    self.availableVideoTracks = availableVideoTracks
    self.availableSubtitleTracks = availableSubtitleTracks
    self.availableAudioTracks = availableAudioTracks
  }
}

public struct IsExternalPlaybackActiveEventPayload: Codable, Sendable {
  public var isExternalPlaybackActive: Bool
  public var oldIsExternalPlaybackActive: Bool?

  public init(isExternalPlaybackActive: Bool = false, oldIsExternalPlaybackActive: Bool? = nil) {
    self.isExternalPlaybackActive = isExternalPlaybackActive
    self.oldIsExternalPlaybackActive = oldIsExternalPlaybackActive
  }
}

public struct PlayToEndEventPayload: Codable, Sendable {
  public init() {}
}

public extension Encodable {
  func toJSONString() -> String {
    let encoder = JSONEncoder()
    guard let data = try? encoder.encode(self),
          let str = String(data: data, encoding: .utf8) else {
      return "{}"
    }
    return str
  }
}
