package app.edumate.server.services

import app.edumate.server.models.notification.Notification

interface OneSignalService {
    suspend fun send(notification: Notification): Boolean

    companion object {
        const val NOTIFICATIONS = "https://onesignal.com/api/v1/notifications"
    }
}
