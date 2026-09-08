import AVFoundation
import Foundation
import UniformTypeIdentifiers

private actor WriteCoordinator {
  func performWrite(
    data: Data,
    offset: Int64,
    fileHandle: MediaFileHandle,
    mediaInfo: MediaInfo?
  ) throws {
    try fileHandle.write(data: data, atOffset: offset)
    let endOffset = offset + Int64(data.count)
    mediaInfo?.addDataRange(newDataRange: (offset, endOffset))
    mediaInfo?.saveToFile()
  }
}

class CachedResource {
  private let url: URL
  private let dataPath: String
  private let fileHandle: MediaFileHandle
  private(set) var mediaInfo: MediaInfo?
  private let writeCoordinator = WriteCoordinator()

  init(dataFileUrl: String, resourceUrl: URL, dataPath: String) {
    self.dataPath = dataPath
    self.url = resourceUrl
    self.fileHandle = MediaFileHandle(filePath: dataFileUrl)
    self.mediaInfo = MediaInfo(at: dataPath + VideoCacheManager.mediaInfoSuffix)
  }

  func onResponseReceived(response: HTTPURLResponse) {
    guard let headers = response.allHeaderFields as? [String: String], mediaInfo == nil, response.statusCode == 200 else {
      return
    }

    mediaInfo = MediaInfo(
      expectedContentLength: response.expectedContentLength,
      mimeType: response.mimeType,
      supportsByteRangeAccess: urlResponseSupportsByteRangeAcces(response),
      headerFields: headers,
      savePath: dataPath + VideoCacheManager.mediaInfoSuffix
    )
    mediaInfo?.saveToFile()
  }

  func fill(forLoadingRequest request: AVAssetResourceLoadingRequest) {
    guard let mediaInfo, let mimeType = mediaInfo.mimeType else {
      return
    }
    let fakeResponse = HTTPURLResponse(url: url, statusCode: 200, httpVersion: nil, headerFields: mediaInfo.headerFields)
    let contentType = UTType(mimeType: mimeType)?.identifier
    if let contentType {
      request.contentInformationRequest?.contentType = contentType
      request.contentInformationRequest?.contentLength = mediaInfo.expectedContentLength
      request.contentInformationRequest?.isByteRangeAccessSupported = mediaInfo.supportsByteRangeAccess
      request.response = fakeResponse
    }
  }

  func writeData(data: Data, offset: Int64) async {
    guard !data.isEmpty else {
      print("[nitro-video] Attempted to write empty data at offset \(offset), skipping")
      return
    }

    guard offset >= 0 else {
      print("[nitro-video] Invalid negative offset: \(offset)")
      return
    }

    do {
      try await writeCoordinator.performWrite(
        data: data,
        offset: offset,
        fileHandle: fileHandle,
        mediaInfo: mediaInfo
      )
    } catch {
      print("[nitro-video] Failed to write at offset \(offset) with the file handle: \(error)")
    }
  }

  func requestData(from: Int64, to: Int64) -> Data? {
    guard from <= to else {
      print("[nitro-video] Invalid range: from=\(from) > to=\(to)")
      return nil
    }

    if canRespondWithData(from: from, to: to) {
      let length64 = to - from + 1
      guard length64 >= 0 && length64 <= Int.max else {
        print("[nitro-video] Requested range length \(length64) exceeds Int.max")
        return nil
      }

      let length = Int(length64)
      return fileHandle.readData(withOffset: from, forLength: length)
    }
    return nil
  }

  func requestBeginningOfData(from: Int64, to: Int64) -> Data? {
    guard let dataRange = mediaInfo?.loadedDataRanges.first(where: { dataStart, dataEnd in
      from >= dataStart && from < dataEnd
    }) else {
      return nil
    }

    let availableLength = dataRange.1 - from
    guard availableLength > 0 else {
      return nil
    }

    guard availableLength <= Int.max else {
      print("[nitro-video] Available length \(availableLength) exceeds Int.max")
      return nil
    }

    return fileHandle.readData(withOffset: from, forLength: Int(availableLength))
  }

  func canRespondWithData(from: Int64, to: Int64) -> Bool {
    guard let loadedDataRanges = mediaInfo?.loadedDataRanges else {
      return false
    }
    let exclusiveEnd = to + 1
    return loadedDataRanges.contains(where: { loadedDataRangeStart, loadedDataRangeEnd in
      from >= loadedDataRangeStart && exclusiveEnd <= loadedDataRangeEnd
    })
  }
}
