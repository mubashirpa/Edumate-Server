package app.edumate.server.di

import app.edumate.server.services.OneSignalService
import app.edumate.server.services.OneSignalServiceImpl
import app.edumate.server.utils.Classroom
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.database.FirebaseDatabase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule =
    module {
        singleOf(::OneSignalServiceImpl) { bind<OneSignalService>() }
        singleOf(::Classroom)
        single<OneSignalService> {
            val apiKey =
                System.getenv("ONE_SIGNAL_API_KEY") ?: throw IllegalStateException("OneSignal api key not found")
            OneSignalServiceImpl(client = get(), apiKey = apiKey)
        }
        single<Firestore> { FirestoreClient.getFirestore() }
        single<FirebaseDatabase> { FirebaseDatabase.getInstance() }
        single<HttpClient> {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
            }
        }
    }
