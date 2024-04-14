package app.edumate.server.plugins

import app.edumate.server.data.remote.OneSignalService
import app.edumate.server.routes.*
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
        courseWorkRouting(classroom, firestore)
        coursesRouting(classroom)
        meetRouting(classroom, firebaseDatabase, oneSignalAppId, oneSignalService)
        studentSubmissionsRouting(classroom, firestore)
        studentsRouting(classroom)
        teachersRouting(classroom)
    }
}
