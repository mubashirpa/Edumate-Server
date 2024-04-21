package app.edumate.server.plugins

import app.edumate.server.routes.*
import app.edumate.server.services.OneSignalService
import app.edumate.server.utils.Classroom
import com.google.cloud.firestore.Firestore
import com.google.firebase.database.FirebaseDatabase
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    classroom: Classroom,
    firebaseDatabase: FirebaseDatabase,
    firestore: Firestore,
    oneSignalAppId: String,
    oneSignalService: OneSignalService,
) {
    routing {
        announcementsRouting(classroom, firestore)
        courseWorkRouting(classroom, firestore, oneSignalAppId, oneSignalService)
        coursesRouting(classroom)
        meetRouting(classroom, firebaseDatabase, oneSignalAppId, oneSignalService)
        studentSubmissionsRouting(classroom, firestore, oneSignalAppId, oneSignalService)
        studentsRouting(classroom)
        teachersRouting(classroom)
        usersRoutes(classroom)
    }
}
