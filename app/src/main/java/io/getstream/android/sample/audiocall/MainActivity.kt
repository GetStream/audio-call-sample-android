package io.getstream.android.sample.audiocall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.android.sample.audiocall.screens.LoadingScreen
import io.getstream.android.sample.audiocall.screens.LoginScreen
import io.getstream.android.sample.audiocall.screens.call.AudioCallScreen
import io.getstream.android.sample.audiocall.videwmodel.AudioCallSampleAppVideModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.streamCallId

class MainActivity : ComponentActivity() {
    // This is just the simplest and fastest way to create the view model without any dependencies
    // In a real app you should utilize a different method of creating the view model.
    private val viewModel: AudioCallSampleAppVideModel =
        AudioCallSampleAppVideModel(AudioCallSampleApp.instance)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.appBackground
                ) {
                    // Data to work with
                    val action = intent.action
                    val callInfo = intent.streamCallId(NotificationHandler.INTENT_EXTRA_CALL_CID)
                    val context = LocalContext.current
                    val userUiState = viewModel.userState
                    val callUiState = viewModel.callUiState

                    // Check if we have call info in the intent (assuming incoming call)
                    if (callInfo != null) {
                        // We have an action triggered with call ID,
                        // we must determine what the action was.
                        when (action) {
                            NotificationHandler.ACTION_ACCEPT_CALL -> {
                                viewModel.accept(context, callInfo)
                            }

                            NotificationHandler.ACTION_REJECT_CALL -> {
                                viewModel.reject(context, callInfo)
                            }

                            NotificationHandler.ACTION_INCOMING_CALL -> {
                                viewModel.incoming(context, callInfo)
                            }
                        }
                    }

                    MainScreen(callUiState = callUiState,
                        userState = userUiState,
                        onLogin = { userId, token ->
                            viewModel.login(context = context, userId = userId, token = token)
                        },
                        onDial = { members ->
                            viewModel.call(context = context, members = members)
                        },
                        onReset = {
                            viewModel.reset()
                        },
                        onReject = {
                            viewModel.leave(it)
                        },
                        onDecline = {
                            viewModel.reject(it)
                        },
                        onCancel = {
                            viewModel.cancel(it)
                        },
                        onAccept = {
                            viewModel.accept(it)
                        })
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    callUiState: AudioCallSampleAppVideModel.CallUiState,
    userState: AudioCallSampleAppVideModel.UserUiState,
    onLogin: (userId: String, token: String?) -> Unit = { _, _ -> },
    onDial: (List<String>) -> Unit = { _ -> },
    onReset: () -> Unit = {},
    onReject: (Call) -> Unit = {},
    onDecline: (Call) -> Unit = {},
    onCancel: (Call) -> Unit = {},
    onAccept: (Call) -> Unit = {}
) {
    when (userState) {
        is AudioCallSampleAppVideModel.UserUiState.Loading -> {
            // Show progress bar
            LoadingScreen()
        }

        is AudioCallSampleAppVideModel.UserUiState.Empty -> {
            // Snow Login
            LoginScreen(onLogin)
        }

        is AudioCallSampleAppVideModel.UserUiState.Actual -> {
            // Show dial screen
            AudioCallScreen(
                callUiState, onDial, onReset, onReject, onDecline, onCancel, onAccept
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    VideoTheme {
        MainScreen(
            userState = AudioCallSampleAppVideModel.UserUiState.Loading,
            callUiState = AudioCallSampleAppVideModel.CallUiState.Undetermined
        )
    }
}