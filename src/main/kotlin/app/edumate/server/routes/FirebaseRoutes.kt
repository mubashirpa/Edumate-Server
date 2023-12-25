package app.edumate.server.routes

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.firebaseRouting(database: Firestore) {
    route("/firestore") {
        get {
            val reference: DocumentReference = database.collection("cities").document("SF")
            val future = reference.get()
            val document = future.get()

            if (document.exists()) {
                call.respondText("${document.data}", status = HttpStatusCode.OK)
            } else {
                call.respondText("No such document!", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}