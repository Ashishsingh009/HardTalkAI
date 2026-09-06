package ai.hardtalk.source

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform