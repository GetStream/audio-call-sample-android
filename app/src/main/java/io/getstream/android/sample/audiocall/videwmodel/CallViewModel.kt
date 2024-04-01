package io.getstream.android.sample.audiocall.videwmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.result.Error
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openapitools.client.models.CallEndedEvent
import org.openapitools.client.models.CallRejectedEvent
import org.openapitools.client.models.VideoEvent
import java.util.UUID

/**
 * View model for the call activity.
 *//*
IMPORTANT:  The ViewModel concept is just one way to implement the application.
            The Call object from the Stream SDK is not dependant on the ViewModel architecture
            and can be used with any pattern.
            Same can be said for the UI state pattern.
            It is possible to utilize state flows
            provided by the Call object directly in your @Composable code.
 */
class CallViewModel : ViewModel() {
    /** Ui state for the call */
    @Stable
    sealed class CallUiState(val call: Call?, val err: io.getstream.result.Error?) {
        /** State is new and not initialized. */
        data object Undetermined : CallUiState(null, null)

        /** Call failed to be created i.e. the SDK can not be used to join the call. */
        class Error(err: io.getstream.result.Error) : CallUiState(null, err)

        /** The call is setup and call actions / events are available via the [Call] class. */
        class Established(call: Call) : CallUiState(call, null)

        /**
         * Notify the UI that the call has ended, e.g. last participant has left
         */
        data object Ended : CallUiState(null, null)
    }

    var callUiState: CallUiState by mutableStateOf(CallUiState.Undetermined)
    private val onCallEvent: VideoEventListener<VideoEvent> = VideoEventListener {
        // Here we can monitor all call events
        when (it) {
            is CallEndedEvent -> {
                callUiState = CallUiState.Ended
            }

            is CallRejectedEvent -> {
                callUiState = CallUiState.Ended
            }
        }
    }

    init {
        val sdkInstance = StreamVideo.instance()
        viewModelScope.launch(Dispatchers.IO) {
            // When we have active call, we will subscribe for the call events.
            sdkInstance.state.activeCall.collectLatest {
                it?.subscribe(onCallEvent)
            }
        }
    }

    fun call(
        // Call id, can come from the intent, or be defined randomly here
        // For incoming call this will be filled from the intent
        // For outgoing calls this will have the default random value.
        cid: StreamCallId = StreamCallId(
            "audio_call", UUID.randomUUID().toString()
        ),
        // Members will be empty for incoming calls, and filled for outgoing calls.
        members: List<String> = emptyList(),
        // Callback for when the call is successfully created or obtained.
        // actions like accept / reject will work
        onSuccess: suspend (Call) -> Unit = { call ->
            callUiState = CallUiState.Established(call)
        },
        // Callback for when the call creation was not sucessfull
        // call can not proceed and actions like accept/join can not work
        onError: suspend (Error) -> Unit = { err ->
            // You can post any errors to the UI if needed.
            //callUiState = CallUiState.Error(err)
        }
    ) = viewModelScope.launch(Dispatchers.IO) {

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

        // Call was established, invoke callback
        result.onSuccessSuspend {
            call.state.settings.value?.audio?.micDefaultOn
            onSuccess(call)
        }

        // Something went wrong, invoke the error callback
        result.onErrorSuspend {
            onError(it)
        }
    }

    /** Leave the call from the parameter. */
    fun leave(call: Call) {
        // Will quietly leave the call, leaving it intact for the other participants.
        call.leave()
        // Update UI state.
        callUiState = CallUiState.Undetermined
    }

    /**
     * Reject the call in the parameter.
     */
    fun reject(call: Call) = viewModelScope.launch(Dispatchers.IO) {
        // If you need to know if the call was successfully rejected
        // monitor the two callbacks and then update UI state for example.
        val rejectOutcome = call.reject()
        rejectOutcome.onSuccess {
            call.leave()
            callUiState = CallUiState.Ended
        }
        rejectOutcome.onError {
            call.leave()
            callUiState = CallUiState.Error(it)
        }
    }

    /** Cancel the outgoing call. */
    fun cancel(call: Call) {
        // Cancel an outgoing call is the same
        // as rejecting the call by the caller.
        reject(call)
    }

    /** Accept an incoming call. */
    fun accept(call: Call) = viewModelScope.launch(Dispatchers.IO) {
        // Since its an incoming call, we can safely assume it is already created
        // no need to set create = true, or ring = true or anything.
        // Accept then join.
        // Check for already active call so we do not have to join twice
        val activeCall = StreamVideo.instance().state.activeCall.value
        if (activeCall != null) {
            if (activeCall.id != call.id) {
                // If the call id is different leave the previous call
                activeCall.leave()
                // Join the call
                call.accept()
                call.join()
            }
        } else {
            // No active call, safe to join
            call.accept()
            call.join()
        }
        callUiState = CallUiState.Established(call)
    }

    /** Accept an incoming call. */
    fun accept(callInfo: StreamCallId) = viewModelScope.launch(Dispatchers.IO) {
        call(
            callInfo,
            onSuccess = { accept(it) },
        )
    }

    /** Reject an incoming call. */
    fun reject(callInfo: StreamCallId) = viewModelScope.launch(Dispatchers.IO) {
        call(
            callInfo,
            onSuccess = { reject(it) },
        )
    }

    /** Update our UI state based on the incomming call. */
    fun incoming(callInfo: StreamCallId) {
        call(callInfo, onSuccess = {
            // Just update the UI with the current incoming call.
            callUiState = CallUiState.Established(it)
        })
    }

    fun end(call: Call) {
        viewModelScope.launch(Dispatchers.IO) {
            call.end()
            callUiState = CallUiState.Ended
        }
    }

    fun end(callInfo: StreamCallId) {
        call(callInfo, onSuccess = { it.end() })
    }
}