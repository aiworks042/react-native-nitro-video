import Foundation

public struct DRMOptions: Codable, Equatable, Sendable {
  public var type: DRMType = .fairplay
  public var licenseServer: String?
  public var headers: [String: String]?
  public var contentId: String?
  public var certificateUrl: URL?
  public var base64CertificateData: String?

  public init(
    type: DRMType = .fairplay,
    licenseServer: String? = nil,
    headers: [String: String]? = nil,
    contentId: String? = nil,
    certificateUrl: URL? = nil,
    base64CertificateData: String? = nil
  ) {
    self.type = type
    self.licenseServer = licenseServer
    self.headers = headers
    self.contentId = contentId
    self.certificateUrl = certificateUrl
    self.base64CertificateData = base64CertificateData
  }
}
