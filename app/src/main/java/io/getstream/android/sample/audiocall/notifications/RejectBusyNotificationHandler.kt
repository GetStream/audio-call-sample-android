package io.getstream.android.sample.audiocall.notifications

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import io.getstream.android.sample.audiocall.utils.BUSY_KEY
import io.getstream.android.sample.audiocall.utils.USER_KEY
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RejectBusyNotificationHandler(private val context: Application) :
    DefaultNotificationHandler(application = context) {

        companion object {
            const val TAG = "RejectBusyNotificationHandler"
        }

    val scope = CoroutineScope(Dispatchers.IO)

    // Called when a Ringing Call arrives. (outgoing or incoming)
    override fun onRingingCall(callId: StreamCallId, callDisplayName: String) {
        // Get stream video instance
        val streamVideo = StreamVideo.instanceOrNull()
        val busy = streamVideo?.let { instance ->
            val activeCall = instance.state.activeCall.value
            Log.d(TAG, "[onRingingCall] callID: $callId")
            if (activeCall != null) {
                Log.d(TAG, "[onRingingCall] there is active call")
                scope.launch {
                    // Get the info for the call object
                    val callDescription = instance.call(callId.type, callId.id)

                    // Get incoming call information from backend
                    val result = callDescription.get()

                    // If we have the info
                    result.onSuccess { newCall ->
                        // New caller
                        val newCaller = newCall.members.filterNot { member ->
                            member.user.id == instance.userId
                        }.first()

                        // Current user in call
                        val currentCallCaller = activeCall.state.members.value.filterNot { member ->
                            member.user.id == instance.userId
                        }.first()

                        if (newCaller.user.id != currentCallCaller.user.id) {
                            Log.d(TAG, "[onRingingCall] - different caller - call super")
                            // If the new caller is different than the current one, proceed as usual
                            super.onRingingCall(callId, callDisplayName)
                        } else {
                            Log.d(TAG,"same caller, don't show notification")
                        }
                    }

                    // If an error occurs
                    result.onError {
                        Log.d(TAG, "[onRingingCall] - could not get call - call super")
                        // Proceed as usual, let the error be handled in the parent
                        super.onRingingCall(callId, callDisplayName)
                    }
                }.invokeOnCompletion {
                    // Just a log to know if the job  completed regardless of outcome
                    Log.d(TAG, "[onRingingCall] call check completed")
                }
                // Active call, we will decide later if we show the notification, for now we return true
                true
            } else {
                Log.d(TAG, "[onRingingCall] No active call")
                // No active call, show the notification
                false
            }
        } ?: false

        // If we are not busy show the notification i.e. call parent
        if (!busy) {
            Log.d(TAG, "[onRingingCall] - not busy - call super")
            super.onRingingCall(callId, callDisplayName)
        }
        // else we do nothing and ignore the since we already sent the busy event
    }

}