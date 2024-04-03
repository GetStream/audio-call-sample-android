package io.getstream.android.sample.audiocall

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.PermissionStatus
import io.getstream.android.sample.audiocall.videwmodel.CallViewModel
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialog
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.VideoEvent
import java.util.UUID
import kotlin.IllegalStateException

open class DefaultCallActivity : ComponentActivity() {
    // Factory and creation
    companion object {
        private const val EXTRA_MEMBERS_ARRAY: String = "members_extra"

        /**
         * Factory method used to build an intent to start this activity.
         *
         * @param context the context.
         * @param cid the Call id
         * @param members list of members
         * @param action android action.
         */
        fun callIntent(
            context: Context, cid: StreamCallId, members: List<String> = emptyList(), action: String
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                // Setup the outgoing call action
                this.action = action
                // Add the generated call ID
                putExtra(NotificationHandler.INTENT_EXTRA_CALL_CID, cid)
                // Setup the members to transfer to the new activity
                val membersArrayList = ArrayList<String>()
                members.forEach { membersArrayList.add(it) }
                putStringArrayListExtra(EXTRA_MEMBERS_ARRAY, membersArrayList)
            }
        }
    }

    // Internal state
    private var subscription: EventSubscription? = null
    private val onSuccessFinish: suspend (Call) -> Unit = {
        finish()
    }
    private val onErrorFinish: suspend (Exception) -> Unit = {
        finish()
    }

    // Platform
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeCallOrFail(savedInstanceState, null)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        initializeCallOrFail(savedInstanceState, persistentState)
    }

    /**
     * Same as [onCreate] but with the [Call] as parameter.
     * The [onCreate] method will crash with [IllegalArgumentException] if the call cannot be loaded
     * or if the intent was created incorrectly, thus arriving in this method means that the call
     * is already available.
     */
    open fun onCreate(
        savedInstanceState: Bundle?, persistentState: PersistableBundle?, call: Call
    ) {

        setContent {
            VideoTheme {
                LaunchPermissionRequest(listOf(RECORD_AUDIO)) {
                    AllPermissionsGranted {
                        // All permissions granted
                        val connection by call.state.connection.collectAsStateWithLifecycle()
                        val duration by call.state.durationInDateFormat.collectAsStateWithLifecycle()

                        LaunchedEffect(key1 = connection) {
                            if (connection == RealtimeConnection.Disconnected) {
                                onSuccessFinish(call)
                            } else if (connection is RealtimeConnection.Failed) {
                                // By default
                                onSuccessFinish(call)
                            }
                        }

                        RingingCallContent(
                            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
                            call = call,
                            onBackPressed = {
                                reject(call)
                            },
                            onAcceptedContent = {
                                AudioCallContent(
                                    call = call,
                                    isMicrophoneEnabled = false,
                                    durationPlaceholder = duration ?: "Calling...",
                                )
                            },
                            onNoAnswerContent = {
                                onCallAction(call, LeaveCall)
                            },
                            onRejectedContent = {
                                onCallAction(call, DeclineCall)
                            },
                            onCallAction = {
                                onCallAction(call, it)
                            },
                        )
                    }
                    SomeGranted { granted, notGranted, showRationale ->
                        // Some of the permissions were granted, you can check which ones.
                        if (showRationale) {
                            NoPermissions()
                        } else {
                            finish()
                        }
                    }
                    NoneGranted {
                        // None of the permissions were granted.
                        if (it) {
                            NoPermissions()
                        } else {
                            finish()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NoPermissions() {
        StreamDialogPositiveNegative(
            content = {
                Text(
                    text = "Some permissions are required",
                    style = TextStyle(
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight(500),
                        color = VideoTheme.colors.basePrimary,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "The app needs access to your microphone.",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 18.5.sp,
                        fontWeight = FontWeight(400),
                        color = VideoTheme.colors.baseSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )
            },
            // Color is for preview only
            style = StreamDialogStyles.defaultDialogStyle(),
            positiveButton = Triple(
                "Settings",
                ButtonStyles.secondaryButtonStyle(StyleSize.S),
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            },
            negativeButton = Triple(
                "Not now",
                ButtonStyles.tertiaryButtonStyle(StyleSize.S),
            ) {
                finish()
            },
        )
    }

    /**
     * Called when a new event for the call is available.
     *
     * @param call the call.
     * @param event the event.
     */
    open fun onCallEvent(call: Call, event: VideoEvent): Boolean {
        // By default none of the events are handled.
        return false
    }

    /**
     * Called when the user invoked some action, like enable/disable speakerphone
     *
     * Some of the actions are handled by default.
     *
     * @param call the call.
     * @param action the action.
     */
    open fun onCallAction(call: Call, action: CallAction): Boolean {
        // None of the actions are handled by default
        return false
    }

    private fun internalOnCallAction(call: Call, action: CallAction) {
        if (!onCallAction(call, action)) {
            when (action) {
                is ToggleCamera -> call.camera.setEnabled(action.isEnabled)
                is ToggleMicrophone -> call.microphone.setEnabled(action.isEnabled)
                is ToggleSpeakerphone -> call.speaker.setEnabled(action.isEnabled)
                is LeaveCall -> {
                    leave(call, onSuccessFinish, onErrorFinish)
                }

                is DeclineCall -> {
                    reject(call, onSuccessFinish, onErrorFinish)
                }

                is CancelCall -> {
                    cancel(call, onSuccess = {
                        finish()
                    }, onError = {
                        finish()
                    })
                }

                is AcceptCall -> {
                    accept(call, onError = onErrorFinish)
                }

                else -> Unit
            }
        }
    }

    private fun internalOnCallEvent(call: Call, event: VideoEvent) {
        if (!onCallEvent(call, event)) {
            when (event) {
                is CallEndedEvent -> {
                    // In any case finish the activity, the call is done for
                    leave(call, onSuccess = {
                        finish()
                    }, onError = {
                        finish()
                    })
                }
            }
        }
    }

    private fun initializeCallOrFail(
        savedInstanceState: Bundle?, persistentState: PersistableBundle?
    ) {
        val cid = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)
        val members =
            intent.getStringArrayListExtra(CallActivity.EXTRA_MEMBERS_ARRAY) ?: emptyList()
        if (cid == null) {
            // Crash early!
            throw IllegalArgumentException("CallActivity started without call ID.")
        }
        call(cid, members, { call ->
            subscription?.dispose()
            subscription = call.subscribe { event ->
                internalOnCallEvent(call, event)
            }
            onCreate(savedInstanceState, persistentState, call)
        }, {
            throw IllegalArgumentException(
                "CallActivity but failed to retrieve call data [cid=$cid]", it
            )
        })
    }

// Call actions
    /**
     * Get a call instance with the members list and call ID.
     *
     * Note: Callbacks are posted on [Dispatchers.Main] dispatcher.
     *
     * @param cid the call ID
     * @param members the call members
     * @param onSuccess callback where the [Call] object is returned
     * @param onError callback when the [Call] was not returned.
     */
    open fun call(
        cid: StreamCallId,
        members: List<String> = emptyList(),
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            // If we have user get or initialize the SDK
            val sdkInstance = StreamVideo.instance()
            // Since the SDk is initialized we can get the user from the SDK.
            val userId = sdkInstance.userId
            // Setup the call with ID
            val call = sdkInstance.call(cid.type, cid.id)
            // Determine the full list of call members
            // Must add own user since the call must contain the full list of users
            val allUsers = members + userId
            // Create the call and if this is successful (flatMap) then join the call()
            val result = call.create(
                // List of all users, containing the caller also
                memberIds = allUsers,
                // If other users will get push notification.
                ring = true
            )

            result.onOutcome(call, onSuccess, onError)
        }
    }

    /**
     * Accept an incoming call.
     *
     * @param call the call to accept.
     * @param onSuccess invoked when the [Call.join] has finished.
     * */
    open fun accept(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Since its an incoming call, we can safely assume it is already created
            // no need to set create = true, or ring = true or anything.
            // Accept then join.
            // Check if we are already in a call
            val activeCall = StreamVideo.instance().state.activeCall.value
            if (activeCall != null) {
                if (activeCall.id != call.id) {
                    // If the call id is different leave the previous call
                    activeCall.leave()
                    // Join the call, only if accept succeeds
                    val result = call.acceptThenJoin()
                    result.onOutcome(call, onSuccess, onError)
                } else {
                    // Already accepted and joined
                    onSuccess?.invoke(call)
                }
            } else {
                // No active call, safe to join
                val result = call.acceptThenJoin()
                result.onOutcome(call, onSuccess, onError)
            }
        }
    }

    /**
     * Reject the call in the parameter.
     *
     * Invokes [Call.reject] then [Call.leave]. Optionally callbacks are invoked.
     *
     * @param call the call to reject
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    open fun reject(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = call.reject()
            result.onOutcome(call, onSuccess, onError)
            call.leave()
        }
    }

    /**
     * Cancel an outgoing call if any.
     * Same as [reject] but can be overridden for different behavior on Outgoing calls.
     *
     * @param call the [Call] to cancel.
     * @param onSuccess optionally get notified if the operation was success.
     * @param onError optionally get notified if the operation failed.
     */
    open fun cancel(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        // Cancel an outgoing call is the same
        // as rejecting the call by the caller.
        reject(call, onSuccess, onError)
    }

    /**
     * Leave the call from the parameter.
     *
     * @param call the call object.
     */
    open fun leave(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Will quietly leave the call, leaving it intact for the other participants.
            try {
                call.leave()
                onSuccess?.invoke(call)
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }
    }

    /**
     * End the call.
     */
    open fun end(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = call.end()
            result.onOutcome(call, { leave(call, onSuccess, onError) }, onError)
        }
    }

    private suspend fun <A : Any> Result<A>.onOutcome(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)? = null,
        onError: (suspend (Exception) -> Unit)? = null,
    ) = withContext(Dispatchers.Main) {
        onSuccess?.let {
            onSuccessSuspend {
                onSuccess(call)
            }
        }
        onError?.let {
            onErrorSuspend {
                onError(Exception(it.message))
            }
        }
    }

    private suspend fun Call.acceptThenJoin() =
        withContext(Dispatchers.IO) { accept().map { join() } }
}