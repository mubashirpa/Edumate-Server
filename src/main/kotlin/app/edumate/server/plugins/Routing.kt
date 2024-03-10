package app.edumate.server.plugins

import app.edumate.server.dao.DAOFacade
import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.routes.*
import com.google.cloud.firestore.Firestore
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    oneSignalService: OneSignalService,
    oneSignalAppId: String,
    firebaseFirestore: Firestore,
    classroom: Classroom,
    daoFacade: DAOFacade,
) {
    routing {
        coursesRouting(classroom)
        courseWorkRouting(daoFacade, classroom)
        firebaseRouting(firebaseFirestore)
        notificationRouting(oneSignalService, oneSignalAppId)
        studentsRouting(classroom)
    }
}
