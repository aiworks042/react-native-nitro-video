import AVFoundation
import Foundation

internal class ContentKeyManager {
  static let contentKeyDelegateQueue = DispatchQueue(label: "nitro.video.ContentKeyDelegateQueue")
  let contentKeySession: AVContentKeySession
  let contentKeyDelegate: ContentKeyDelegate

  init() {
    contentKeySession = AVContentKeySession(keySystem: .fairPlayStreaming)
    contentKeyDelegate = ContentKeyDelegate()
    contentKeySession.setDelegate(contentKeyDelegate, queue: ContentKeyManager.contentKeyDelegateQueue)
  }

  func addContentKeyRequest(videoSource: VideoSource, asset: AVContentKeyRecipient) {
    contentKeyDelegate.videoSource = videoSource
    contentKeySession.addContentKeyRecipient(asset)
  }
}
