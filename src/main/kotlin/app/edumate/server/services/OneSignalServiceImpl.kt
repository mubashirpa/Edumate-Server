package app.edumate.server.services

import app.edumate.server.models.notification.Request
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class OneSignalServiceImpl(
    private val client: HttpClient,
    private val apiKey: String,
) : OneSignalService {
    override suspend fun send(request: Request): Boolean {
        return try {
            client.post(OneSignalService.NOTIFICATIONS) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Basic $apiKey")
                setBody(request)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
