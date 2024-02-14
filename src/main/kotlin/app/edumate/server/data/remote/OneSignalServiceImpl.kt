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
                setBody(notification)
                header("accept", ContentType.Application.Json)
                header("Authorization", "Basic $apiKey")
                contentType(ContentType.Application.Json)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
