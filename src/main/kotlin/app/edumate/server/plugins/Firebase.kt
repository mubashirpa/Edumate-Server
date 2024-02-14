package app.edumate.server.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream

fun Application.configureFirebase() {
    val databaseUrl = environment.config.propertyOrNull("firebase.database_url")?.getString().orEmpty()
    val refreshToken = FileInputStream("src/main/resources/edu-mate-app-firebase-adminsdk.json")

    val options =
        FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(refreshToken))
            .setDatabaseUrl(databaseUrl)
            .build()

    FirebaseApp.initializeApp(options)
}
