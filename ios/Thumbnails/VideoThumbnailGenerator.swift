import AVFoundation
import CoreMedia
import UIKit

internal func generateThumbnails(asset: AVAsset, times: [CMTime], options: VideoThumbnailOptions?) async throws -> [VideoThumbnail] {
  let generator = AVAssetImageGenerator(asset: asset)

  generator.appliesPreferredTrackTransform = true
  generator.requestedTimeToleranceAfter = .zero
  generator.maximumSize = options?.getMaxSize() ?? .zero

  if times.allSatisfy({ $0 < asset.duration }) {
    generator.requestedTimeToleranceBefore = .zero
  }
  return try await generateThumbnails(generator: generator, times: times)
}

private func generateThumbnails(generator: AVAssetImageGenerator, times: [CMTime]) async throws -> [VideoThumbnail] {
  if #available(iOS 16, tvOS 16, *) {
    return try await generator
      .images(for: times)
      .reduce(into: [VideoThumbnail]()) { thumbnails, result in
        let thumbnail = VideoThumbnail(result.image, requestedTime: result.requestedTime, actualTime: result.actualTime)
        thumbnails.append(thumbnail)
      }
  }
  return try await VideoThumbnailLegacyGenerator(generator: generator, times: times)
    .reduce(into: [VideoThumbnail]()) { thumbnails, thumbnail in
      thumbnails.append(thumbnail)
    }
}

internal struct VideoThumbnailLegacyGenerator: AsyncSequence, AsyncIteratorProtocol {
  typealias Element = VideoThumbnail

  let generator: AVAssetImageGenerator
  let times: [CMTime]
  var currentIndex: Int = 0

  mutating func next() async throws -> Element? {
    guard currentIndex < times.count, !Task.isCancelled else {
      return nil
    }
    let requestedTime = times[currentIndex]
    var actualTime = CMTime.zero
    let image = try generator.copyCGImage(at: requestedTime, actualTime: &actualTime)

    currentIndex += 1

    return VideoThumbnail(image, requestedTime: requestedTime, actualTime: actualTime)
  }

  func makeAsyncIterator() -> Self {
    return self
  }
}
