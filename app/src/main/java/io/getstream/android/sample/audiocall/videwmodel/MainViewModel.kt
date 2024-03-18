package io.getstream.android.sample.audiocall.videwmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.android.sample.audiocall.AudioCallSampleApp
import io.getstream.android.sample.audiocall.CallActivity
import io.getstream.android.sample.audiocall.storage.UserData
import io.getstream.android.sample.audiocall.storage.UserStorage
import io.getstream.result.Error
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
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
class MainViewModel(application: Application) : ViewModel() {

    // State models
    /** Ui State for the user. */
    sealed class UserUiState(val userData: UserData, val streamUser: User) {
        /** Indicate loading in progress. */
        data object Loading : UserUiState(UserData.NoUser, User())

        /** Indicates empty user */
        data object Empty : UserUiState(UserData.NoUser, User())

        /** Contains actual user data. */
        class Actual(userData: UserData, streamUser: User) : UserUiState(userData, streamUser)
    }

    // State
    var userState: UserUiState by mutableStateOf(UserUiState.Loading)

    init {
        viewModelScope.launch {
            UserStorage.user(application.applicationContext).collectLatest {
                userState = when (it) {
                    is UserData.NoUser -> UserUiState.Empty
                    is UserData.AudioCallUser -> {
                        // Load the user also from stream SDK.
                        val sdkInstance = AudioCallSampleApp.instance.streamVideo(it)
                        val user = sdkInstance.user
                        if (user.isValid()) {
                            // Return a UI state with all the user data
                            UserUiState.Actual(it, user)
                        } else {
                            // If something is invalid return an Empty user
                            // to enforce re-login
                            UserUiState.Empty
                        }
                    }
                }
            }
        }
    }

    fun placeCall(
        // Needed to load some data
        context: Context,
        // Call id, can come from the intent, or be defined randomly here
        // For incming call this will be filled from the intent
        // For outgoing calls this will have the default random value.
        cid: StreamCallId = StreamCallId(
            "audio_call", "123"
        ),
        // Members will be empty for incoming calls, and filled for outgoing calls.
        members: List<String> = emptyList(),
    ) = viewModelScope.launch(Dispatchers.IO) {
        val intent = CallActivity.placeCallIntent(context, cid, members)
        context.startActivity(intent)
    }

    fun logout(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // Simply delete the user data to simulate logout
        UserStorage.delete(context)
        // Update UI state
        userState = UserUiState.Empty
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
}