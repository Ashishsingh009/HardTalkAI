package ai.hardtalk.source.voice

/** Official OpenAI Realtime HTTPS hosts that may receive a minted client secret. */
val ALLOWED_OPENAI_REALTIME_HOSTS = setOf("api.openai.com")

data class ParsedHttpUrl(
    val scheme: String,
    val host: String,
    val port: Int?,
    val path: String,
)

fun parseHttpUrl(raw: String): ParsedHttpUrl? {
    val trimmed = raw.trim()
    val schemeSep = trimmed.indexOf("://")
    if (schemeSep <= 0) return null
    val scheme = trimmed.substring(0, schemeSep).lowercase()
    if (scheme != "http" && scheme != "https") return null
    val rest = trimmed.substring(schemeSep + 3)
    if (rest.isEmpty()) return null
    val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
        .let { if (it < 0) rest.length else it }
    val authority = rest.substring(0, authorityEnd)
    if (authority.isEmpty() || '@' in authority || authority.startsWith('[')) return null
    val host: String
    val port: Int?
    val colon = authority.indexOf(':')
    if (colon >= 0) {
        host = authority.substring(0, colon).lowercase()
        port = authority.substring(colon + 1).toIntOrNull() ?: return null
    } else {
        host = authority.lowercase()
        port = null
    }
    if (host.isEmpty() || '.' !in host) return null
    val remainder = if (authorityEnd < rest.length) rest.substring(authorityEnd) else "/"
    val path = remainder.substringBefore('?').substringBefore('#')
        .ifEmpty { "/" }
    return ParsedHttpUrl(scheme = scheme, host = host, port = port, path = path)
}

fun isAllowlistedRealtimeUrl(realtimeUrl: String): Boolean {
    val parsed = parseHttpUrl(realtimeUrl) ?: return false
    if (parsed.scheme != "https") return false
    if (parsed.host !in ALLOWED_OPENAI_REALTIME_HOSTS) return false
    if (parsed.port != null && parsed.port != 443) return false
    return parsed.path.startsWith("/v1/realtime")
}

fun requireAllowlistedRealtimeUrl(realtimeUrl: String): String {
    if (!isAllowlistedRealtimeUrl(realtimeUrl)) {
        throw IllegalArgumentException("Realtime URL is not an allowlisted OpenAI HTTPS host")
    }
    return realtimeUrl
}
