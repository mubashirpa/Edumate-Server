package app.edumate.server.routes

import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.models.Notification
import app.edumate.server.models.NotificationMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notificationRouting(service: OneSignalService) {
    route("/notification") {
        get {
            val title = call.parameters["title"] ?: return@get call.respondText(
                "Missing title",
                status = HttpStatusCode.BadRequest
            )
            val description = call.parameters["description"] ?: return@get call.respondText(
                "Missing description",
                status = HttpStatusCode.BadRequest
            )

            val success = service.send(
                Notification(
                    includeExternalUserIds = listOf("All"),
                    includedSegments = listOf("All"),
                    contents = NotificationMessage(en = title),
                    headings = NotificationMessage(en = description),
                    appId = OneSignalService.ONESIGNAL_APP_ID
                )
            )

            if (success) {
                call.respondText("Notification send successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Notification send failed", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}