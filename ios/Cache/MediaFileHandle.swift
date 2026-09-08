import Foundation

internal class MediaFileHandle {
  private let filePath: String
  private let lock = NSLock()
  private var readHandle: FileHandle?
  private var writeHandle: FileHandle?

  var attributes: [FileAttributeKey: Any]? {
    do {
      return try FileManager.default.attributesOfItem(atPath: filePath)
    } catch let error as NSError {
      print("[nitro-video] An error occurred while reading the file attributes at \(filePath) error: \(error)")
    }
    return nil
  }

  var fileSize: Int {
    return attributes?[.size] as? Int ?? 0
  }

  private var fileUrl: URL? {
    URL(string: filePath)
  }

  init(filePath: String) {
    self.filePath = filePath

    if let fileUrl {
      VideoCacheManager.shared.registerOpenFile(at: fileUrl)
    }

    if !FileManager.default.fileExists(atPath: filePath) {
      let fileCreated = FileManager.default.createFile(atPath: filePath, contents: nil, attributes: nil)
      if !fileCreated {
        print("[nitro-video] Failed to create cache file at: \(filePath)")
      }
    }

    self.readHandle = FileHandle(forReadingAtPath: filePath)
    self.writeHandle = FileHandle(forWritingAtPath: filePath)

    if readHandle == nil {
      print("[nitro-video] Failed to open file for reading at: \(filePath)")
    }
    if writeHandle == nil {
      print("[nitro-video] Failed to open file for writing at: \(filePath)")
    }
  }

  deinit {
    if let fileUrl {
      VideoCacheManager.shared.unregisterOpenFile(at: fileUrl)
    }
    guard FileManager.default.fileExists(atPath: filePath) else {
      return
    }

    close()
  }

  func readData(withOffset offset: Int64, forLength length: Int) -> Data? {
    lock.lock()
    defer { lock.unlock() }

    guard let readHandle = readHandle else {
      print("[nitro-video] Read handle not available for file at: \(filePath)")
      return nil
    }

    guard offset >= 0 else {
      print("[nitro-video] Invalid negative offset: \(offset)")
      return nil
    }

    let currentSize = fileSize
    guard offset <= currentSize else {
      print("[nitro-video] Offset \(offset) exceeds file size \(currentSize)")
      return nil
    }

    do {
      try readHandle.seek(toOffset: UInt64(offset))
      let data = try readHandle.read(upToCount: length) ?? Data()

      if data.count < length && offset + Int64(data.count) < currentSize {
        print("[nitro-video] Read \(data.count) bytes but expected \(length) bytes")
      }

      return data
    } catch {
      print("[nitro-video] Failed to read data at offset \(offset): \(error)")
      return nil
    }
  }

  func write(data: Data, atOffset offset: Int64) throws {
    lock.lock()
    defer { lock.unlock() }

    guard let writeHandle = writeHandle else {
      throw VideoCacheException("Failed to write data to cache file handle: Write handle not available for file at: \(filePath)")
    }

    guard offset >= 0 else {
      throw VideoCacheException("Failed to write data to cache file handle: Invalid negative offset: \(offset)")
    }

    try writeHandle.seek(toOffset: UInt64(offset))
    try writeHandle.write(contentsOf: data)
    try writeHandle.synchronize()
  }

  func append(data: Data) {
    lock.lock()
    defer { lock.unlock() }
    guard let writeHandle = writeHandle else {
      return
    }

    do {
      try writeHandle.seekToEnd()
      try writeHandle.write(contentsOf: data)
      try writeHandle.synchronize()
    } catch {
      print("[nitro-video] Failed to append data to the file at \(filePath): \(error)")
    }
  }

  func close() {
    try? readHandle?.close()
    try? writeHandle?.close()
  }
}
