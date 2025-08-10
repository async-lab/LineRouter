package club.asynclab.linerouter

enum class StatusCode(val code: Int) {
    OK(200),
    RATE_LIMIT(429),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503)
}