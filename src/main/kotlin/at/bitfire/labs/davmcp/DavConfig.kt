package at.bitfire.labs.davmcp

data class DavConfig(
    val calendarUrl: String,
    val username: String,
    val password: String
) {
    companion object {
        fun fromEnvironment(): DavConfig {
            fun requireEnv(name: String): String =
                System.getenv(name)?.takeIf { it.isNotBlank() }
                    ?: error("Required environment variable $name is not set")

            return DavConfig(
                calendarUrl = requireEnv("CALDAV_URL"),
                username = requireEnv("CALDAV_USERNAME"),
                password = requireEnv("CALDAV_PASSWORD")
            )
        }
    }
}
