import Foundation

class MediaInfo: Codable {
  var expectedContentLength: Int64
  var supportsByteRangeAccess: Bool
  var mimeType: String?
  var headerFields: [String: String]?
  var savePath: String

  private let lock = NSLock()
  private var loadedDataRangesArr: [[Int64]] = []

  private(set) var loadedDataRanges: [(Int64, Int64)] {
    get {
      lock.lock()
      defer { lock.unlock() }
      return loadedDataRangesArrayToTuple()
    }
    set {
      lock.lock()
      defer { lock.unlock() }
      loadedDataRangesArr = loadedDataRangesTupleToArray(newValue)
    }
  }

  private enum CodingKeys: String, CodingKey {
    case expectedContentLength, supportsByteRangeAccess, mimeType, loadedDataRangesArr, headerFields, savePath
  }

  init(expectedContentLength: Int64, mimeType: String?, supportsByteRangeAccess: Bool, headerFields: [String: String]?, savePath: String) {
    self.mimeType = mimeType
    self.supportsByteRangeAccess = supportsByteRangeAccess
    self.expectedContentLength = expectedContentLength
    self.headerFields = headerFields
    self.savePath = savePath

    if let url = URL(string: savePath) {
      VideoCacheManager.shared.registerOpenFile(at: url)
    }
  }

  deinit {
    if let url = URL(string: savePath) {
      VideoCacheManager.shared.unregisterOpenFile(at: url)
    }
  }

  convenience init?(data: Data, dataPath: String) {
    do {
      let mediaInfo = try JSONDecoder().decode(MediaInfo.self, from: data)
      self.init(
        expectedContentLength: mediaInfo.expectedContentLength,
        mimeType: mediaInfo.mimeType,
        supportsByteRangeAccess: mediaInfo.supportsByteRangeAccess,
        headerFields: mediaInfo.headerFields,
        savePath: mediaInfo.savePath
      )
      self.loadedDataRanges = mediaInfo.loadedDataRanges
    } catch {
      return nil
    }
  }

  convenience init?(at path: String) {
    guard FileManager.default.fileExists(atPath: path), let mediaInfoData = FileManager.default.contents(atPath: path) else {
      return nil
    }
    self.init(data: mediaInfoData, dataPath: path)
  }

  convenience init?(forResourceUrl url: URL, variantKey: String = "") {
    guard let filePath = VideoAsset.pathForUrl(url: url, fileExtension: url.pathExtension, variantKey: variantKey) else {
      return nil
    }
    let mediaInfoPath = filePath + VideoCacheManager.mediaInfoSuffix
    self.init(at: mediaInfoPath)
  }

  func addDataRange(newDataRange: (Int64, Int64)) {
    lock.lock()
    defer { lock.unlock() }

    guard newDataRange.1 > newDataRange.0 else {
      print("[nitro-video] Invalid range: [\(newDataRange.0), \(newDataRange.1)) - end must be > start")
      return
    }

    let currentRanges = loadedDataRangesArrayToTuple()
    var i = 0
    var merged = [(Int64, Int64)]()

    while i < currentRanges.count && currentRanges[i].1 < newDataRange.0 {
      merged.append(currentRanges[i])
      i += 1
    }

    var newStart = newDataRange.0
    var newEnd = newDataRange.1
    while i < currentRanges.count && currentRanges[i].0 <= newDataRange.1 {
      newStart = min(newStart, currentRanges[i].0)
      newEnd = max(newEnd, currentRanges[i].1)
      i += 1
    }
    merged.append((newStart, newEnd))

    while i < currentRanges.count {
      merged.append(currentRanges[i])
      i += 1
    }
    loadedDataRangesArr = loadedDataRangesTupleToArray(merged)
  }

  func encodeToData() -> Data? {
    do {
      return try JSONEncoder().encode(self)
    } catch {
      print("[nitro-video] Error encoding MediaInfo object: \(error)")
      return nil
    }
  }

  func saveToFile() {
    do {
      guard let data = self.encodeToData() else {
        print("[nitro-video] Failed to encode MediaInfo for saving at: \(savePath)")
        return
      }

      let tempPath = savePath + ".tmp"
      let tempURL = URL(fileURLWithPath: tempPath)
      let finalURL = URL(fileURLWithPath: savePath)

      let parentDirectory = finalURL.deletingLastPathComponent()
      if !FileManager.default.fileExists(atPath: parentDirectory.path) {
        try FileManager.default.createDirectory(at: parentDirectory, withIntermediateDirectories: true, attributes: nil)
      }

      if !FileManager.default.fileExists(atPath: tempPath) && FileManager.default.fileExists(atPath: savePath) {
        try FileManager.default.copyItem(at: finalURL, to: tempURL)
      }

      try data.write(to: tempURL, options: .atomic)

      if FileManager.default.fileExists(atPath: savePath) {
        try FileManager.default.removeItem(at: finalURL)
      }

      try FileManager.default.moveItem(at: tempURL, to: finalURL)
    } catch {
      print("[nitro-video] Failed to save media info at: \(savePath), error: \(error)")
    }
  }

  private func loadedDataRangesArrayToTuple() -> [(Int64, Int64)] {
    let filteredDataRanges = loadedDataRangesArr.filter { rangeArray in
      rangeArray.count == 2
    }

    return filteredDataRanges.map { rangeArray in
      (rangeArray[0], rangeArray[1])
    }
  }

  private func loadedDataRangesTupleToArray(_ loadedDataRanges: [(Int64, Int64)]) -> [[Int64]] {
    return loadedDataRanges.map { from, to in
      return [from, to]
    }
  }
}
