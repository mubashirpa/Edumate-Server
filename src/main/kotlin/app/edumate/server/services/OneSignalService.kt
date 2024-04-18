package app.edumate.server.services

import app.edumate.server.models.notification.Request

interface OneSignalService {
    suspend fun send(request: Request): Boolean

    companion object {
        const val NOTIFICATIONS = "https://onesignal.com/api/v1/notifications"
    }
}
