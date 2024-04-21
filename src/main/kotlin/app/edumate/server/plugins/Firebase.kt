package app.edumate.server.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

fun configureFirebase() {
    val databaseUrl =
        System.getenv("FIREBASE_DATABASE_URL") ?: throw IllegalStateException("Firebase database url not found")
    val refreshToken = FileInputStream("src/main/resources/edu-mate-app-firebase-adminsdk.json")

    val options =
        FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(refreshToken))
            .setDatabaseUrl(databaseUrl)
            .build()

    FirebaseApp.initializeApp(options)
}
