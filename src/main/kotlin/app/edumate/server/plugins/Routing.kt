package app.edumate.server.plugins

import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.routes.firebaseRouting
import app.edumate.server.routes.notificationRouting
import com.google.cloud.firestore.Firestore
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    oneSignalService: OneSignalService,
    oneSignalAppId: String,
    firebaseFirestore: Firestore
) {
    routing {
        notificationRouting(oneSignalService, oneSignalAppId)
        firebaseRouting(firebaseFirestore)
    }
}
