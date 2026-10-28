package ai.hardtalk.source.data.api

/** Official app identifier required by `POST /api/voice/session`. */
const val HARDTALK_CLIENT_HEADER = "X-HardTalk-Client"
const val HARDTALK_CLIENT_VALUE = "hardtalk-app"

/** Random per-process token for first-party funnel counts. Not an account id. */
const val HARDTALK_SESSION_HEADER = "X-HardTalk-Session"
