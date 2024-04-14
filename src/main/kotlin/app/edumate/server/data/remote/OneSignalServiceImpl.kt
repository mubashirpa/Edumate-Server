package app.edumate.server.data.remote

import app.edumate.server.models.notification.Notification
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class OneSignalServiceImpl(
    private val client: HttpClient,
    private val apiKey: String,
) : OneSignalService {
    override suspend fun send(notification: Notification): Boolean {
        return try {
            client.post(OneSignalService.NOTIFICATIONS) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Basic $apiKey")
                setBody(notification)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
