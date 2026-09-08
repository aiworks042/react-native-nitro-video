import AVFoundation
import CoreServices
import CryptoKit
import Foundation
import MobileCoreServices

internal func getRangeFromResponse(response: HTTPURLResponse) -> (Int?, Int?) {
  let fullString = (response.allHeaderFields["Content-Range"] ?? response.allHeaderFields["content-range"]) as? String
  let fullStringComponents = fullString?.components(separatedBy: "/").first?.components(separatedBy: " ")
  guard let rangeStringComponents = fullStringComponents?[safe: 1]?.components(separatedBy: "-") else {
    return (nil, nil)
  }
  let first = rangeStringComponents.first ?? ""
  let second = rangeStringComponents.count > 1 ? rangeStringComponents[1] : ""

  return (Int(first), Int(second))
}

internal func getRangeFromRequest(request: URLRequest) -> (Int?, Int?) {
  guard let fullString = request.allHTTPHeaderFields?["Range"] ?? request.allHTTPHeaderFields?["range"] else {
    return (nil, nil)
  }
  let rangeString = fullString.replacingOccurrences(of: "bytes=", with: "")
  let rangeStringComponents = rangeString.split(separator: "-", maxSplits: 1, omittingEmptySubsequences: false)
  let first = rangeStringComponents.first ?? ""
  let second = rangeStringComponents.count > 1 ? rangeStringComponents[1] : ""

  let firstInt = first.isEmpty ? nil : Int(first)
  let secondInt = second.isEmpty ? nil : Int(second)

  return (firstInt, secondInt)
}

internal func mimeTypeToExtension(mimeType: String?) -> String? {
  guard let mimeType else {
    return nil
  }

  guard let mimeUTI = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeType as CFString, nil)?.takeUnretainedValue() else {
    return nil
  }
  return UTTypeCopyPreferredTagWithClass(mimeUTI, kUTTagClassFilenameExtension)?.takeRetainedValue() as? String
}

internal func urlResponseSupportsByteRangeAcces(_ response: HTTPURLResponse) -> Bool {
  if let acceptRanges = response.allHeaderFields["Accept-Ranges"] as? String ?? response.allHeaderFields["accept-ranges"] as? String {
    return acceptRanges.lowercased().contains("bytes")
  }
  return response.statusCode == 206
}

internal extension Int {
  var MB: Int { return self * 1024 * 1024 }
}

internal extension Array {
  subscript(safe index: Int) -> Element? {
    return indices.contains(index) ? self[index] : nil
  }
}

internal extension Data {
  func subdata(request: URLRequest, response: HTTPURLResponse) -> Data? {
    let (requestStart, requestEnd) = getRangeFromRequest(request: request)
    let (responseStart, responseEnd) = getRangeFromResponse(response: response)

    guard let responseStart, let responseEnd, let requestStart else {
      return nil
    }

    guard responseStart <= responseEnd, (responseStart...responseEnd).contains(requestStart) else {
      return nil
    }

    let startIndex = requestStart - responseStart

    guard startIndex >= 0 && startIndex < self.count else {
      return nil
    }

    var endIndex: Int

    if let requestEnd = requestEnd {
      let requestedLength = requestEnd - requestStart + 1
      endIndex = startIndex + requestedLength
    } else {
      endIndex = self.count
    }

    endIndex = Swift.min(endIndex, self.count)

    guard startIndex <= endIndex else {
      return nil
    }

    return self.subdata(in: startIndex..<endIndex)
  }
}

internal extension URL {
  func withScheme(_ scheme: String) -> URL? {
    var components = URLComponents(url: self, resolvingAgainstBaseURL: false)
    components?.scheme = scheme
    return components?.url
  }
}
