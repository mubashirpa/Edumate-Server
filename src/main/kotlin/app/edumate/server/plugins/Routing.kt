package app.edumate.server.plugins

import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.routes.notificationRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    service: OneSignalService,
    appId: String
) {
    routing {
        notificationRouting(service, appId)
    }
}
