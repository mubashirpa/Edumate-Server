package app.edumate.server.routes

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.QuerySnapshot
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.firebaseRouting(firebaseFirestore: Firestore) {
    route("/v1/courses") {
        get("{userId}/{courseStates?}/{pageSize?}/{pageToken?}/{studentId?}/{teacherId?}") {
            val userId =
                call.parameters["userId"] ?: return@get call.respondText(
                    "You must specify an userId.",
                    status = HttpStatusCode.BadRequest,
                )

            val query: ApiFuture<QuerySnapshot> =
                firebaseFirestore.collection("courses")
                    .whereArrayContains("teacherGroupId", userId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(10).get()
            val querySnapshot = query.get()
            val documents = querySnapshot.documents

            if (documents.isNotEmpty()) {
                for (document in documents) {
                    println(document.id + " => " + document.data["name"])
                }
                call.respondText("$documents", status = HttpStatusCode.OK)
            } else {
                call.respondText("No such document!", status = HttpStatusCode.NotFound)
            }
        }
    }
}
