import AVFoundation
import CoreServices
import Foundation
import UIKit
import UniformTypeIdentifiers

final class ResourceLoaderDelegate: NSObject, AVAssetResourceLoaderDelegate, URLSessionDelegate, URLSessionDataDelegate, URLSessionTaskDelegate {
  private let url: URL
  private let saveFilePath: String
  private let fileExtension: String
  private let cachedResource: CachedResource
  private let urlRequestHeaders: [String: String]?
  private let cacheRequestHeaders: [String: String]
  private let variantKey: String
  private var responseAllowsStorage: Bool = true
  private var policyEvaluated: Bool = false
  internal var onError: ((Error) -> Void)?

  private var cachableRequests: SynchronizedHashTable<CachableRequest> = SynchronizedHashTable()
  private var session: URLSession?

  private static let requestTimeoutInterval: Double = 5

  private var pathWithExtension: String {
    let ext = mimeTypeToExtension(mimeType: cachedResource.mediaInfo?.mimeType)
    if let ext, self.fileExtension.isEmpty {
      return self.saveFilePath + ".\(ext)"
    }
    return self.saveFilePath
  }

  init(
    url: URL,
    saveFilePath: String,
    fileExtension: String,
    urlRequestHeaders: [String: String]?,
    cacheRequestHeaders: [String: String],
    variantKey: String
  ) {
    self.url = url
    self.saveFilePath = saveFilePath
    self.fileExtension = fileExtension
    self.urlRequestHeaders = urlRequestHeaders
    self.cacheRequestHeaders = cacheRequestHeaders
    self.variantKey = variantKey
    cachedResource = CachedResource(dataFileUrl: saveFilePath, resourceUrl: url, dataPath: saveFilePath)
    super.init()

    let delegateQueue = OperationQueue()
    delegateQueue.maxConcurrentOperationCount = 1
    self.session = URLSession(configuration: .default, delegate: self, delegateQueue: delegateQueue)
  }

  deinit {
    session?.invalidateAndCancel()
    session = nil
  }

  // MARK: - AVAssetResourceLoaderDelegate

  func resourceLoader(_ resourceLoader: AVAssetResourceLoader, shouldWaitForLoadingOfRequestedResource loadingRequest: AVAssetResourceLoadingRequest) -> Bool {
    processLoadingRequest(loadingRequest: loadingRequest)
    return true
  }

  func resourceLoader(_ resourceLoader: AVAssetResourceLoader, didCancel loadingRequest: AVAssetResourceLoadingRequest) {
    cachableRequest(by: loadingRequest)?.dataTask.cancel()
  }

  // MARK: - URLSessionDelegate

  func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
    guard let currentRequest = dataTask.currentRequest,
      let response = dataTask.response as? HTTPURLResponse,
      let cachableRequest = cachableRequest(by: dataTask) else {
      return
    }

    let dataRequest = cachableRequest.dataRequest
    let requestedOffset = dataRequest.requestedOffset
    let currentOffset = dataRequest.currentOffset
    let length = dataRequest.requestedLength

    let subdata = data.subdata(request: currentRequest, response: response) ?? data
    cachableRequest.onReceivedData(data: subdata)

    if dataRequest.requestsAllDataToEndOfResource {
      guard currentOffset >= requestedOffset else {
        print("[nitro-video] Current offset (\(currentOffset)) < requested offset (\(requestedOffset))")
        return
      }

      let currentDataResponseOffset = Int(currentOffset - requestedOffset)
      guard currentDataResponseOffset >= 0 && currentDataResponseOffset <= cachableRequest.receivedData.count else {
        print("[nitro-video] Invalid offset: \(currentDataResponseOffset), receivedData.count: \(cachableRequest.receivedData.count)")
        return
      }

      let currentDataResponseLength = cachableRequest.receivedData.count - currentDataResponseOffset
      guard currentDataResponseLength >= 0 else {
        return
      }

      let endOffset = currentDataResponseOffset + currentDataResponseLength
      guard endOffset <= cachableRequest.receivedData.count else {
        print("[nitro-video] End offset (\(endOffset)) exceeds receivedData.count (\(cachableRequest.receivedData.count))")
        return
      }

      let subdata = cachableRequest.receivedData.subdata(in: currentDataResponseOffset..<endOffset)
      dataRequest.respond(with: subdata)
    } else if currentOffset >= requestedOffset && currentOffset - requestedOffset < cachableRequest.receivedData.count {
      let rangeStart = Int(currentOffset - requestedOffset)
      let rangeLength = min(cachableRequest.receivedData.count - rangeStart, length)

      guard rangeStart >= 0 && rangeStart < cachableRequest.receivedData.count && rangeLength > 0 else {
        return
      }

      let endOffset = rangeStart + rangeLength
      guard endOffset <= cachableRequest.receivedData.count else {
        print("[nitro-video] End offset (\(endOffset)) exceeds receivedData.count (\(cachableRequest.receivedData.count))")
        return
      }

      let subdata = cachableRequest.receivedData.subdata(in: rangeStart..<endOffset)
      dataRequest.respond(with: subdata)
    }
  }

  func urlSession(
    _ session: URLSession,
    dataTask: URLSessionDataTask,
    didReceive response: URLResponse,
    completionHandler: @escaping (URLSession.ResponseDisposition) -> Void
  ) {
    evaluateCachePolicy(forResponse: response)
    if let cachedDataRequest = cachableRequest(by: dataTask) {
      cachedDataRequest.response = response
      if cachedDataRequest.loadingRequest.contentInformationRequest != nil {
        fillInContentInformationRequest(forDataRequest: cachedDataRequest)
        cachedDataRequest.loadingRequest.response = response
        cachedDataRequest.loadingRequest.finishLoading()
        cachedDataRequest.dataTask.cancel()
        cachableRequests.remove(cachedDataRequest)
      }
    }
    completionHandler(.allow)
  }

  private func evaluateCachePolicy(forResponse response: URLResponse) {
    guard !policyEvaluated, let httpResponse = response as? HTTPURLResponse else {
      return
    }
    policyEvaluated = true
    var headers: [String: String] = [:]
    for (key, value) in httpResponse.allHeaderFields {
      if let keyString = key as? String, let valueString = value as? String {
        headers[keyString] = valueString
      }
    }
    let policy = CachePolicy.evaluate(responseHeaders: headers, statusCode: httpResponse.statusCode)
    responseAllowsStorage = policy.isCacheable
    if !responseAllowsStorage {
      evictStoredFiles()
      return
    }
    CacheVariantIndex.recordVariant(
      forUrl: url,
      storageKey: variantKey,
      requestHeaders: cacheRequestHeaders,
      fileExtension: fileExtension,
      policy: policy
    )
  }

  private func evictStoredFiles() {
    try? FileManager.default.removeItem(atPath: saveFilePath)
    try? FileManager.default.removeItem(atPath: saveFilePath + VideoCacheManager.mediaInfoSuffix)
  }

  func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    guard let cachedDataRequest = cachableRequest(by: task) else {
      return
    }

    if let error = error as? URLError, error.code == URLError.cancelled || error.code == URLError.networkConnectionLost {
      if responseAllowsStorage {
        cachedDataRequest.saveData(to: cachedResource)
      }
      cachedDataRequest.loadingRequest.finishLoading(with: error)
    } else if error == nil {
      if responseAllowsStorage {
        cachedDataRequest.saveData(to: cachedResource)
      }
      cachedDataRequest.loadingRequest.finishLoading()
    } else {
      cachedDataRequest.loadingRequest.finishLoading(with: error)
    }
    cachableRequests.remove(cachedDataRequest)
  }

  private func processLoadingRequest(loadingRequest: AVAssetResourceLoadingRequest) {
    let (remainingRequest, dataReceived) = attemptToRespondFromCache(forRequest: loadingRequest)

    if dataReceived != nil && dataReceived?.isEmpty != true && remainingRequest == nil {
      return
    }

    var request = remainingRequest ?? createUrlRequest()

    if remainingRequest == nil {
      addRangeHeaderFields(loadingRequest: loadingRequest, urlRequest: &request)
    }

    guard let session else {
      return
    }

    let dataTask = session.dataTask(with: request)

    if loadingRequest.dataRequest != nil {
      let cachableRequest = CachableRequest(loadingRequest: loadingRequest, dataTask: dataTask, dataRequest: loadingRequest.dataRequest!)
      if let dataReceived {
        cachableRequest.onReceivedData(data: dataReceived)
      }
      cachableRequests.add(cachableRequest)
    } else {
      print("[nitro-video] ResourceLoaderDelegate has received a loading request without a data request")
    }
    dataTask.resume()
  }

  private func fillInContentInformationRequest(forDataRequest request: CachableRequest?) {
    guard let response = request?.response as? HTTPURLResponse else {
      return
    }

    request?.loadingRequest.contentInformationRequest?.contentLength = response.expectedContentLength
    request?.loadingRequest.contentInformationRequest?.isByteRangeAccessSupported = true

    if let mimeType = response.mimeType, isSupported(mimeType: mimeType) {
      let rawUti = UTType(mimeType: mimeType)?.identifier
      request?.loadingRequest.contentInformationRequest?.contentType = rawUti ?? response.mimeType
      cachedResource.onResponseReceived(response: response)
    } else {
      onError?(VideoCacheUnsupportedFormatException(response.mimeType ?? ""))
    }
  }

  private func attemptToRespondFromCache(forRequest loadingRequest: AVAssetResourceLoadingRequest) -> (request: URLRequest?, dataReceived: Data?) {
    guard let dataRequest = loadingRequest.dataRequest else {
      return (nil, nil)
    }

    let from = dataRequest.requestedOffset
    let to = from + Int64(dataRequest.requestedLength) - 1

    if let cachedData = cachedResource.requestData(from: from, to: to) {
      if loadingRequest.contentInformationRequest != nil {
        cachedResource.fill(forLoadingRequest: loadingRequest)
      }
      loadingRequest.dataRequest?.respond(with: cachedData)
      loadingRequest.finishLoading()
      return (nil, cachedData)
    }

    if let partialData = cachedResource.requestBeginningOfData(from: from, to: to) {
      if loadingRequest.contentInformationRequest != nil {
        cachedResource.fill(forLoadingRequest: loadingRequest)
      }
      loadingRequest.dataRequest?.respond(with: partialData)

      var request = createUrlRequest()
      if loadingRequest.contentInformationRequest == nil {
        if loadingRequest.dataRequest?.requestsAllDataToEndOfResource == true {
          let requestedOffset = dataRequest.requestedOffset
          request.setValue("bytes=\(Int(requestedOffset) + partialData.count)-", forHTTPHeaderField: "Range")
        } else if let dataRequest = loadingRequest.dataRequest {
          let requestedOffset = dataRequest.requestedOffset
          let requestedLength = dataRequest.requestedLength
          let from = Int(requestedOffset) + partialData.count
          let to = from + requestedLength - partialData.count - 1
          request.setValue("bytes=\(from)-\(to)", forHTTPHeaderField: "Range")
        }
      }
      return (request, partialData)
    }

    return (nil, nil)
  }

  private func addRangeHeaderFields(loadingRequest: AVAssetResourceLoadingRequest, urlRequest: inout URLRequest) {
    guard let dataRequest = loadingRequest.dataRequest, loadingRequest.contentInformationRequest == nil else {
      return
    }

    if dataRequest.requestsAllDataToEndOfResource {
      let requestedOffset = dataRequest.requestedOffset
      urlRequest.setValue("bytes=\(requestedOffset)-", forHTTPHeaderField: "Range")
      return
    }

    let requestedOffset = dataRequest.requestedOffset
    let requestedLength = Int64(dataRequest.requestedLength)
    urlRequest.setValue("bytes=\(requestedOffset)-\(requestedOffset + requestedLength - 1)", forHTTPHeaderField: "Range")
  }

  private func isSupported(mimeType: String?) -> Bool {
    return mimeType?.starts(with: "video/") ?? false
  }

  private func createUrlRequest() -> URLRequest {
    var request = URLRequest(url: url, cachePolicy: .useProtocolCachePolicy)
    request.timeoutInterval = Self.requestTimeoutInterval

    self.urlRequestHeaders?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }
    return request
  }

  private func cachableRequest(by loadingRequest: AVAssetResourceLoadingRequest) -> CachableRequest? {
    return cachableRequests.allObjects.first(where: {
      $0.loadingRequest == loadingRequest
    })
  }

  private func cachableRequest(by task: URLSessionTask) -> CachableRequest? {
    return cachableRequests.allObjects.first(where: {
      $0.dataTask == task
    })
  }
}
