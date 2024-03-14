package app.edumate.server.plugins

import app.edumate.server.dao.DAOFacade
import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.routes.*
import com.google.cloud.firestore.Firestore
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    classroom: Classroom,
    daoFacade: DAOFacade,
    firestore: Firestore,
    oneSignalAppId: String,
    oneSignalService: OneSignalService,
) {
    routing {
        coursesRouting(classroom)
        courseWorkRouting(classroom, daoFacade, firestore)
        notificationRouting(oneSignalAppId, oneSignalService)
        studentsRouting(classroom)
    }
}
