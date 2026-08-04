package io.getstream.android.sample.audiocall.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.android.push.PushDevice
import io.getstream.android.push.PushProvider
import io.getstream.android.push.firebase.FirebaseMessagingDelegate
import io.getstream.result.Result
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    companion object {
        /** Used as a configuration [providerName] for the Firebase config on Stream website. */
        // This field value should be equal to the configuration name on your stream Dashboard.
        const val FIREBASE_CONFIG_NAME_ON_DASHBOARD = "firebase-config-from-dashboard"
    }

    override fun onNewToken(token: String) {
        // Update device's token on Stream backend
        coroutineScope.launch {
            val streamVideoInstance = StreamVideo.instanceOrNull()
            if (streamVideoInstance != null) {
                val result : Result<Device> =
                    streamVideoInstance.createDevice(
                        PushDevice(
                            token,
                            PushProvider.FIREBASE,
                            FIREBASE_CONFIG_NAME_ON_DASHBOARD
                        )
                    )
                if (result.isFailure) {
                    //Retry to create Device
                }
            } else {
                /**
                 * StreamVideo is not initialized, you cannot do anything here.
                 * You can save the above token in shared-preference and
                 * update the token to Stream-Backend once streamVideo's Instance is ready
                 */
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            //This will forward the PN's message to Stream SDK and sdk can render the notification
            if (!FirebaseMessagingDelegate.handleRemoteMessage(message)) {
                // RemoteMessage was not for stream and needs further processing
                Log.d("Firebase", "PN was not for stream")
            }
        } catch (exception: IllegalStateException) {
            // StreamVideo was not initialized, you can do nothing here, about Stream SDK
            // Maybe log some errors
        }
    }
}