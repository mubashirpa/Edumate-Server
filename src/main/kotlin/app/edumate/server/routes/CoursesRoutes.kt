package app.edumate.server.routes

import app.edumate.server.models.courses.Course
import app.edumate.server.models.courses.CoursesDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.io.InputStreamReader

fun Route.coursesRouting() {
    val coursesStorage: MutableList<Course> = fetchCourses().toMutableList()

    route("/v1/courses") {
        post {
            val course = call.receive<Course>()
            coursesStorage.add(course)
            call.respond(
                status = HttpStatusCode.Created,
                message = course,
            )
        }
        delete("/{id}") {
            val id =
                call.parameters["id"] ?: return@delete call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            if (coursesStorage.removeIf { it.id == id }) {
                call.respondText(text = "The course with id $id was deleted", status = HttpStatusCode.Accepted)
            } else {
                call.respondText(text = "No course with id $id", status = HttpStatusCode.NotFound)
            }
        }
        get("/{id}") {
            val id =
                call.parameters["id"] ?: return@get call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            val course =
                coursesStorage.find { it.id == id } ?: return@get call.respondText(
                    text = "No course with id $id",
                    status = HttpStatusCode.NotFound,
                )
            call.respond(course)
        }
        get {
            if (coursesStorage.isNotEmpty()) {
                val courses = CoursesDto(courses = coursesStorage)
                call.respond(courses)
            } else {
                call.respondText(text = "No courses found", status = HttpStatusCode.OK)
            }
        }
        put("/{id}") {
            val id =
                call.parameters["id"] ?: return@put call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            val index = coursesStorage.indexOfFirst { it.id == id }

            if (index == -1) {
                call.respondText(text = "No course with id $id", status = HttpStatusCode.NotFound)
            } else {
                val course = call.receive<Course>()
                coursesStorage[index] = course
                call.respond(course)
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

private fun fetchCourses(): List<Course> {
    val database = FileInputStream("src/main/resources/courses-db.json")
    val data = InputStreamReader(database).readText()
    return json.decodeFromString(data)
}
