package app.edumate.server

import app.edumate.server.data.remote.OneSignalServiceImpl
import app.edumate.server.plugins.configureRouting
import app.edumate.server.plugins.configureSerialization
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    val apiKey = environment.config.propertyOrNull("onesignal.api_key")?.getString().orEmpty()
    val appId = environment.config.propertyOrNull("onesignal.app_id")?.getString().orEmpty()
    val service = OneSignalServiceImpl(client, apiKey)

    configureRouting(service, appId)
}