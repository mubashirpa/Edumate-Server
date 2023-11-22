package app.edumate.server.data.remote

import app.edumate.server.models.Notification

interface OneSignalService {

    suspend fun send(notification: Notification): Boolean

    companion object {
        const val NOTIFICATIONS = "https://onesignal.com/api/v1/notifications"
    }
}