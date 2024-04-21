package app.edumate.server.routes

import app.edumate.server.models.userProfiles.UserProfile
import app.edumate.server.utils.Classroom
import app.edumate.server.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.usersRoutes(classroom: Classroom) {
    val usersStorage = classroom.usersStorage

    route("v1/users") {
        post {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@post call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@post call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }
            try {
                FirebaseUtils.getUserIdFromToken(token)
            } catch (e: FirebaseAuthException) {
                when (e.authErrorCode.name) {
                    "EXPIRED_ID_TOKEN" -> return@post call.respondText(
                        text = "The access token is expired",
                        status = HttpStatusCode.Unauthorized,
                    )

                    "REVOKED_ID_TOKEN" -> return@post call.respondText(
                        text = "The access token has been revoked",
                        status = HttpStatusCode.Unauthorized,
                    )

                    else -> return@post call.respondText(
                        text = "The provided access token is not valid",
                        status = HttpStatusCode.Unauthorized,
                    )
                }
            }
            val user = call.receive<UserProfile>()

            usersStorage.add(user)
            call.respond(
                status = HttpStatusCode.Created,
                message = user,
            )
        }
    }
}
