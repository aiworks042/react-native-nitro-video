import Foundation

class VideoCacheManager {
  static let defaultMaxCacheSize = 1_024_000_000 // 1GB
  static let defaultAutoCleanCache = true
  static let expoVideoCacheScheme = "nitro-video-cache"
  static let expoVideoCacheDirectory = "nitro-video-cache"
  static let mediaInfoSuffix = "&mediaInfo"

  static let shared = VideoCacheManager()
  private let defaults = UserDefaults.standard

  private let maxCacheSizeKey = "\(VideoCacheManager.expoVideoCacheScheme)/maxCacheSize"

  private var openFiles: Set<URL> = Set()
  private let openFilesLock = NSLock()

  let cacheQueue = DispatchQueue(label: "\(VideoCacheManager.expoVideoCacheScheme)-dispatch-queue")

  var maxCacheSize: Int {
    defaults.maybeInteger(forKey: maxCacheSizeKey) ?? Self.defaultMaxCacheSize
  }

  func registerOpenFile(at url: URL) {
    openFilesLock.lock()
    defer { openFilesLock.unlock() }
    openFiles.insert(url)
  }

  func unregisterOpenFile(at url: URL) {
    openFilesLock.lock()
    defer { openFilesLock.unlock() }
    openFiles.remove(url)
  }

  func setMaxCacheSize(newSize: Int) throws {
    if VideoManager.shared.hasRegisteredPlayers {
      throw VideoCacheException("Cannot change the cache size while there are active players")
    }

    defaults.setValue(newSize, forKey: maxCacheSizeKey)
    ensureCacheSize()
  }

  func ensureCacheSize() {
    cacheQueue.async { [weak self] in
      guard let self else {
        return
      }

      do {
        try self.limitCacheSize(to: self.maxCacheSize)
      } catch {
        print("[nitro-video] Failed to auto clean video cache: \(error)")
      }
    }
  }

  func clearAllCache() async throws {
    return try await withCheckedThrowingContinuation { continuation in
      cacheQueue.async { [weak self] in
        do {
          try self?.deleteAllFilesInCacheDirectory()
          continuation.resume()
        } catch {
          continuation.resume(throwing: error)
        }
      }
    }
  }

  func ensureCacheIntegrity(forSavePath videoFilePath: String) {
    let mediaInfoPath = videoFilePath + Self.mediaInfoSuffix
    let videoFileExists = FileManager.default.fileExists(atPath: videoFilePath)
    let mediaInfoExists = FileManager.default.fileExists(atPath: mediaInfoPath)

    if mediaInfoExists && !videoFileExists {
      try? FileManager.default.removeItem(atPath: mediaInfoPath)
    }
  }

  func getCacheDirectorySize() -> Int64 {
    guard let folderUrl = getCacheDirectory() else {
      return 0
    }
    let fileManager = FileManager.default
    var totalSize: Int64 = 0

    guard let enumerator = fileManager.enumerator(at: folderUrl, includingPropertiesForKeys: [.fileSizeKey], options: .skipsHiddenFiles) else {
      return 0
    }

    for case let fileURL as URL in enumerator {
      guard let fileAttributes = try? fileURL.resourceValues(forKeys: [.fileSizeKey]) else {
        continue
      }
      if let fileSize = fileAttributes.fileSize {
        totalSize += Int64(fileSize)
      }
    }

    return totalSize
  }

  private func limitCacheSize(to maxSize: Int) throws {
    let allFileURLs = getVideoFilesUrls()

    var totalSize: Int64 = 0
    var fileInfo = [(url: URL, size: Int64, accessDate: Date)]()

    for url in allFileURLs {
      let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
      let fileSize = attributes[.size] as? Int64 ?? 0
      let accessDate = try url.resourceValues(forKeys: [.contentAccessDateKey]).contentAccessDate ?? Date.distantPast
      totalSize += fileSize
      fileInfo.append((url: url, size: fileSize, accessDate: accessDate))
    }

    if totalSize <= maxSize {
      return
    }

    let deletableFileInfo = fileInfo.filter { !fileIsOpen(url: $0.url) }
    let sortedFileInfo = deletableFileInfo.sorted { $0.accessDate < $1.accessDate }

    for fileInfo in sortedFileInfo {
      if totalSize <= maxSize {
        continue
      }
      try removeVideoAndMimeTypeFile(at: fileInfo.url)
      totalSize -= fileInfo.size
    }
  }

  private func deleteAllFilesInCacheDirectory() throws {
    if VideoManager.shared.hasRegisteredPlayers {
      throw VideoCacheException("Cannot clear cache while there are active players")
    }

    guard let cacheDirectory = getCacheDirectory() else {
      return
    }

    let fileUrls = try FileManager.default.contentsOfDirectory(at: cacheDirectory, includingPropertiesForKeys: nil, options: [])

    for fileUrl in fileUrls {
      try removeVideoAndMimeTypeFile(at: fileUrl)
    }
  }

  private func removeVideoAndMimeTypeFile(at fileUrl: URL) throws {
    let mimeTypeFileUrl = URL(string: "\(fileUrl.relativeString)\(Self.mediaInfoSuffix)")
    try FileManager.default.removeItem(at: fileUrl)
    if let mimeTypeFileUrl, FileManager.default.fileExists(atPath: mimeTypeFileUrl.relativePath) {
      try FileManager.default.removeItem(at: mimeTypeFileUrl)
    }
  }

  private func isAuxiliaryCacheFile(_ url: URL) -> Bool {
    let s = url.absoluteString
    return s.hasSuffix(Self.mediaInfoSuffix) || s.hasSuffix(CacheVariantIndex.fileSuffix)
  }

  private func getCacheDirectory() -> URL? {
    let cacheDirs = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)
    if let cacheDir = cacheDirs.first {
      return cacheDir.appendingPathComponent(VideoCacheManager.expoVideoCacheDirectory)
    }
    return nil
  }

  private func getVideoFilesUrls() -> [URL] {
    guard let videoCacheDir = getCacheDirectory() else {
      print("[nitro-video] Failed to get the video cache directory.")
      return []
    }
    let fileUrls = (try? FileManager.default.contentsOfDirectory(
      at: videoCacheDir,
      includingPropertiesForKeys: [.contentAccessDateKey, .contentModificationDateKey],
      options: .skipsHiddenFiles)
    ) ?? []
    return fileUrls.filter { !isAuxiliaryCacheFile($0) }
  }

  private func fileIsOpen(url: URL) -> Bool {
    openFilesLock.lock()
    defer { openFilesLock.unlock() }
    return openFiles.contains(url) || openFiles.contains { $0.relativePath == url.relativePath }
  }
}

private extension UserDefaults {
  func exists(forKey key: String) -> Bool {
    return Self.standard.object(forKey: key) != nil
  }

  func maybeInteger(forKey key: String) -> Int? {
    Self.standard.exists(forKey: key) ? Self.standard.integer(forKey: key) : nil
  }
}
