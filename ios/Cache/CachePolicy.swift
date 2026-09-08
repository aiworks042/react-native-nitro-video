import Foundation

struct CachePolicy {
  let varyHeaders: [String]
  let isCacheable: Bool
  let allowsAuthorizedReuse: Bool

  static func evaluate(responseHeaders: [String: String], statusCode: Int) -> CachePolicy {
    guard statusCode == 200 || statusCode == 206 else {
      return CachePolicy(varyHeaders: [], isCacheable: false, allowsAuthorizedReuse: false)
    }

    let vary = splitRespectingQuotes(headerValue(responseHeaders, name: "vary"))
    let cacheControl = directiveNames(in: splitRespectingQuotes(headerValue(responseHeaders, name: "cache-control")))

    if vary.contains(where: { $0.trimmingCharacters(in: .whitespacesAndNewlines) == "*" }) {
      return CachePolicy(varyHeaders: [], isCacheable: false, allowsAuthorizedReuse: false)
    }

    let allowsAuth = cacheControl.contains("public")
      || cacheControl.contains("must-revalidate")
      || cacheControl.contains("s-maxage")

    let normalizedVary = vary.map { $0.lowercased() }.filter { $0 != "*" }.sorted()
    return CachePolicy(
      varyHeaders: Array(Set(normalizedVary)).sorted(),
      isCacheable: true,
      allowsAuthorizedReuse: allowsAuth
    )
  }

  private static func headerValue(_ headers: [String: String], name: String) -> String {
    let lower = name.lowercased()
    for (key, value) in headers where key.lowercased() == lower {
      return value
    }
    return ""
  }

  private static func splitRespectingQuotes(_ value: String) -> [String] {
    var tokens: [String] = []
    var current = ""
    var inQuotes = false
    for char in value {
      if char == "\"" {
        inQuotes.toggle()
        current.append(char)
      } else if char == "," && !inQuotes {
        let trimmed = current.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { tokens.append(trimmed) }
        current.removeAll(keepingCapacity: true)
      } else {
        current.append(char)
      }
    }
    let trimmed = current.trimmingCharacters(in: .whitespacesAndNewlines)
    if !trimmed.isEmpty { tokens.append(trimmed) }
    return tokens
  }

  private static func directiveNames(in tokens: [String]) -> Set<String> {
    var names: Set<String> = []
    for token in tokens {
      let nameEnd = token.firstIndex(of: "=") ?? token.endIndex
      let name = token[..<nameEnd].trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
      if !name.isEmpty { names.insert(name) }
    }
    return names
  }
}
