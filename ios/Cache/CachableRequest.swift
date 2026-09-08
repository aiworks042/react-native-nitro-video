import AVFoundation
import Foundation

class CachableRequest: Equatable, Hashable {
  let loadingRequest: AVAssetResourceLoadingRequest
  let dataTask: URLSessionDataTask
  var dataRequest: AVAssetResourceLoadingDataRequest
  var response: URLResponse?
  private(set) var receivedData = Data()
  private let dataOffset: Int64

  init(loadingRequest: AVAssetResourceLoadingRequest, dataTask: URLSessionDataTask, dataRequest: AVAssetResourceLoadingDataRequest) {
    self.loadingRequest = loadingRequest
    self.dataTask = dataTask
    self.dataRequest = dataRequest
    self.dataOffset = dataRequest.requestedOffset
  }

  func onReceivedData(data: Data) {
    receivedData.append(data)
  }

  func saveData(to cachedResource: CachedResource) {
    Task { [self] in
      await cachedResource.writeData(data: receivedData, offset: dataOffset)
    }
  }

  static func == (lhs: CachableRequest, rhs: CachableRequest) -> Bool {
    return lhs === rhs
  }

  func hash(into hasher: inout Hasher) {
    hasher.combine(ObjectIdentifier(self))
  }
}
