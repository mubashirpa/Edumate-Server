package app.edumate.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        anyHost()
    }
}
