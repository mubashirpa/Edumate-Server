package app.edumate.server

import app.edumate.server.data.remote.OneSignalServiceImpl
import app.edumate.server.plugins.configureCors
import app.edumate.server.plugins.configureFirebase
import app.edumate.server.plugins.configureRouting
import app.edumate.server.plugins.configureSerialization
import com.google.firebase.cloud.FirestoreClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureCors()
    configureSerialization()
    configureFirebase()

    val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }
    val oneSignalApiKey = environment.config.propertyOrNull("onesignal.api_key")?.getString().orEmpty()
    val oneSignalAppId = environment.config.propertyOrNull("onesignal.app_id")?.getString().orEmpty()
    val oneSignalService = OneSignalServiceImpl(client, oneSignalApiKey)
    val firebaseFirestore = FirestoreClient.getFirestore()

    configureRouting(
        oneSignalService,
        oneSignalAppId,
        firebaseFirestore,
    )
}
