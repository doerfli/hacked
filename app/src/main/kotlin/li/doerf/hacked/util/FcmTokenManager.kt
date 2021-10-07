package li.doerf.hacked.util

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import org.joda.time.DateTime

class FcmTokenManager {

    companion object {
        private const val TAG = "FcmTokenManager"
        private const val FCM_TOKEN = "FcmToken"
        private const val FCM_TOKEN_TIMESTAMP = "FcmTokenTimestamp"

        fun getDeviceToken(context: Context): String {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            val token = settings.getString(FCM_TOKEN, null)
            val tokenTimestamp = settings.getLong(FCM_TOKEN_TIMESTAMP, 0)

            if (token != null && tokenTimestamp > 0 && (DateTime.now().minusDays(28).millis < tokenTimestamp)) {
                Log.d(TAG, "already got token:  $token")
                return token
            }

            Log.d(TAG, "retrieving token")
            val newToken = Tasks.await(FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }
            }))

            val newTokenTimestamp = DateTime.now().millis
            Log.d(TAG, "updating settings with token $newToken and timestamp $newTokenTimestamp")
            settings.edit().putString(FCM_TOKEN, newToken).putLong(FCM_TOKEN_TIMESTAMP, newTokenTimestamp).commit()
            return newToken
        }
    }

}