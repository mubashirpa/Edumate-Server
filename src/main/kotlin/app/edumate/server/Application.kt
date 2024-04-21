package app.edumate.server

import app.edumate.server.plugins.*
import app.edumate.server.services.OneSignalService
import app.edumate.server.utils.Classroom
import com.google.cloud.firestore.Firestore
import com.google.firebase.database.FirebaseDatabase
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureCors()
    configureSerialization()
    configureKoin()
    configureFirebase()

    val oneSignalAppId = System.getenv("ONE_SIGNAL_APP_ID") ?: throw IllegalStateException("OneSignal app id not found")
    val oneSignalService by inject<OneSignalService>()
    val firestore by inject<Firestore>()
    val firebaseDatabase by inject<FirebaseDatabase>()
    val classroom by inject<Classroom>()

    configureRouting(
        classroom = classroom,
        firebaseDatabase = firebaseDatabase,
        firestore = firestore,
        oneSignalAppId = oneSignalAppId,
        oneSignalService = oneSignalService,
    )
}
