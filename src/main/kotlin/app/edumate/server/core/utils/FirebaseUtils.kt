package app.edumate.server.core.utils

import com.google.firebase.auth.FirebaseAuth

object FirebaseUtils {
    fun getUserIdFromToken(idToken: String): String {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)
        val uid = decodedToken.uid
        return uid
    }
}
