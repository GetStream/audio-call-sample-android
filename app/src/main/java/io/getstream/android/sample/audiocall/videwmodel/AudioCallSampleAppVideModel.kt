package io.getstream.android.sample.audiocall.videwmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.android.sample.audiocall.AudioCallSampleApp
import io.getstream.android.sample.audiocall.storage.UserData
import io.getstream.android.sample.audiocall.storage.UserStorage
import io.getstream.result.Error
import io.getstream.result.flatMap
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A single combined view model for simplicity.
 *//*
IMPORTANT:  The ViewModel concept is just one way to implement the application.
            The Call object from the Stream SDK is not dependant on the ViewModel architecture
            and can be used with any pattern.
            Same can be said for the UI state pattern.
            It is possible to utilize state flows
            provided by the Call object directly in your @Composable code.
 */
class AudioCallSampleAppVideModel(application: Application) : ViewModel() {

    // State models
    /** Ui State for the user. */
    sealed class UserUiState(userData: UserData) {
        /** Indicate loading in progress. */
        data object Loading : UserUiState(UserData.NoUser)

        /** Indicates empty user */
        data object Empty : UserUiState(UserData.NoUser)

        /** Contains actual user data. */
        class Actual(userData: UserData) : UserUiState(userData)
    }

    /** Ui state for the call */
    sealed class CallUiState(val call: Call?, val err: io.getstream.result.Error?) {
        /** State is new and not initialized. */
        data object Undetermined : CallUiState(null, null)

        /** Call failed to be created i.e. the SDK can not be used to join the call. */
        class Error(err: io.getstream.result.Error) : CallUiState(null, err)

        /** The call is setup and call actions / events are available via the [Call] class. */
        class Established(call: Call) : CallUiState(call, null)
    }

    // State
    var userState: UserUiState by mutableStateOf(UserUiState.Loading)
    var callUiState: CallUiState by mutableStateOf(CallUiState.Undetermined)

    init {
        viewModelScope.launch {
            UserStorage.user(application.applicationContext).collectLatest {
                userState = when (it) {
                    is UserData.NoUser -> UserUiState.Empty
                    is UserData.AudioCallUser -> UserUiState.Actual(it)
                }
            }
        }
    }

    fun call(
        // Needed to load some data
        context: Context,
        // Call id, can come from the intent, or be defined randomly here
        // For incming call this will be filled from the intent
        // For outgoing calls this will have the default random value.
        cid: StreamCallId = StreamCallId(
            "default", UUID.randomUUID().toString()
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
            callUiState = CallUiState.Error(err)
        }
    ) = viewModelScope.launch(Dispatchers.IO) {
        // Load current user
        val user = UserStorage.loadUser(context)

        if (user !is UserData.NoUser) {
            // If we have user get or initialize the SDK
            val sdkInstance = AudioCallSampleApp.instance.streamVideo(user)
            // Setup the call with ID
            val call = sdkInstance.call(cid.type, cid.id)
            // Determine the full list of call members
            // Must add own user since the call must contain the full list of users
            val allUsers = members + user.userId
            // Create the call and if this is successful (flatMap) then join the call()
            val result = call.create(
                // List of all users, containing the caller also
                memberIds = allUsers,
                // If other users will get push notification.
                ring = true
            )

            // Call was established, invoke callback
            result.onSuccessSuspend {
                onSuccess(call)
            }

            // Something went wrong, invoke the error callback
            result.onErrorSuspend {
                onError(it)
            }
        }
    }

    fun logout(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // Simply delete the user data to simulate logout
        UserStorage.delete(context)
    }

    /**
     * Login the user.
     */
    fun login(context: Context, userId: String, token: String?): Job {
        // We'll generate a new dev token if we do not manually input the token.
        val actualToken = token.takeUnless {
            it.isNullOrBlank()
        } ?: StreamVideo.devToken(userId)
        return viewModelScope.launch {
            // Store the entered credentials to simulate logged in session
            UserStorage.store(
                context, UserData.AudioCallUser(userId, actualToken)
            )
        }
    }

    /**
     * Reset back the Call UI state to Undetermined so we can see the dialer.
     */
    fun reset() {
        when (callUiState) {
            is CallUiState.Established -> {
                // Leave any potential call we might have if we are resetting the UI state.
                callUiState.call?.leave()
            }

            else -> {
                // Do nothing
            }
        }
        callUiState = CallUiState.Undetermined
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
        // Reject the call
        val rejectOutcome = call.reject()

        // If you need to know if the call was successfully rejected
        // monitor the two callbacks and then update UI state for example.
        rejectOutcome.onSuccess {
            callUiState = CallUiState.Undetermined
        }
        rejectOutcome.onError {
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
        call.join()
        callUiState = CallUiState.Established(call)
    }

    /** Accept an incoming call. */
    fun accept(context: Context, callInfo: StreamCallId) = viewModelScope.launch(Dispatchers.IO) {
        call(
            context, callInfo,
            onSuccess = {
                // Accept then join.
                // This will notify the caller the call was accepted and join the current user.
                it.accept()
                it.join()
                callUiState = CallUiState.Established(it)
            },
        )
    }

    /** Reject an incoming call. */
    fun reject(context: Context, callInfo: StreamCallId) = viewModelScope.launch(Dispatchers.IO) {
        call(
            context, callInfo,
            onSuccess = {
                // Reject the call
                // This will notify the caller the call was rejected then reset the UI.
                it.reject()
                callUiState = CallUiState.Undetermined
            },
        )
    }

    /** Update our UI state based on the incomming call. */
    fun incoming(context: Context, callInfo: StreamCallId) {
        call(context, callInfo, onSuccess = {
            // Just update the UI with the current incoming call.
            callUiState = CallUiState.Established(it)
        })
    }
}