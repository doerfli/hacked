package li.doerf.hacked.util

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging

class FcmTokenManager {

    companion object {
        private const val TAG = "FcmTokenManager"
        private lateinit var token: String

        fun getDeviceToken(): String {
            if (this::token.isInitialized) {
                Log.d(TAG, "already got token:  $token")
                return token
            }

            Log.d(TAG, "retrieving token")
            return Tasks.await(FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                this.token = task.result
            }))
        }
    }

}