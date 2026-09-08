import Foundation

extension FourCharCode {
  func toCorrectedString() -> String {
    switch toString() {
    case "avc1":
      return "avc"
    case "hev1":
      return "hevc"
    default:
      return self.toString()
    }
  }

  func toString() -> String {
    let bytes: [CChar] = [
      CChar((self >> 24) & 0xff),
      CChar((self >> 16) & 0xff),
      CChar((self >> 8) & 0xff),
      CChar(self & 0xff),
      0
    ]
    let result = String(cString: bytes)
    return result.trimmingCharacters(in: .whitespaces)
  }
}
