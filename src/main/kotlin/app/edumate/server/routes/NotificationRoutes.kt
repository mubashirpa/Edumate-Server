package app.edumate.server.routes

import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.models.Notification
import app.edumate.server.models.NotificationMessage
import app.edumate.server.models.NotificationRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notificationRouting(service: OneSignalService) {
    route("/notification") {
        post {
            val notificationRequest = call.receive<NotificationRequest>()

            val success = service.send(
                Notification(
                    includedSegments = listOf(""),
                    includeExternalUserIds = notificationRequest.userIds,
                    contents = NotificationMessage(en = notificationRequest.description),
                    headings = NotificationMessage(en = notificationRequest.title),
                    appId = OneSignalService.ONESIGNAL_APP_ID
                )
            )

            if (success) {
                call.respondText("Notification send successfully.", status = HttpStatusCode.OK)
            } else {
                call.respondText("Notification send failed.", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}