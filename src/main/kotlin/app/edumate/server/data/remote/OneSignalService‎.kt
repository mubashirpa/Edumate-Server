package app.edumate.server.data.remote

import app.edumate.server.models.Notification

interface OneSignalService {

    suspend fun send(notification: Notification): Boolean

    companion object {
        const val ONESIGNAL_APP_ID = "e3e1c903-e850-4f1a-b750-e9daaa2c2a55"
        const val NOTIFICATIONS = "https://onesignal.com/api/v1/notifications"
    }
}