package io.getstream.android.sample.audiocall.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import io.getstream.android.sample.audiocall.AudioCallSampleApp
import io.getstream.android.sample.audiocall.ui.screens.MainScreen
import io.getstream.android.sample.audiocall.videwmodel.MainViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    // This is just the simplest and fastest way to create the view model without any dependencies
    // In a real app you should utilize a different method of creating the view model.
    private val viewModel: MainViewModel =
        MainViewModel(AudioCallSampleApp.instance)

    private val config: StreamCallActivityConfiguration = StreamCallActivityConfiguration(
        closeScreenOnCallEnded = false,
        canSkiPermissionRationale = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the app is in foreground during an incoming call, the StreamCallActivity would be launched.
        showComposeCallActivityOnIncomingCall()

        // Proceed with set content
        setContent {
            VideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.basePrimary
                ) {
                    val context = LocalContext.current
                    val userUiState = viewModel.userState
                    MainScreen(
                        userState = userUiState,
                        onLogin = { userId, token ->
                            viewModel.login(context = context, userId = userId, token = token)
                        },
                        onLogout = {
                            viewModel.logout(context = context)
                        },
                        onDial = { members ->
                            val intent = StreamCallActivity.callIntent(
                                this,
                                StreamCallId("audio_call", UUID.randomUUID().toString()),
                                members,
                                true,
                                action = NotificationHandler.ACTION_OUTGOING_CALL,
                                // use ComposeStreamCallActivity::class.java for default
                                clazz = CustomCallActivity::class.java,
                                configuration = config
                            )
                            startActivity(intent)
                        })
                }
            }
        }
    }

    /*
    Monitors the ringingCall and if any, starts the default call activity.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun showComposeCallActivityOnIncomingCall() {
        lifecycleScope.launch {
            StreamVideo.instanceState.flatMapLatest { instance ->
                instance?.state?.ringingCall ?: flowOf(null)
            }.collectLatest { call ->
                if (call != null) {
                    lifecycleScope.launch {
                        // Monitor the ringingState on a non-null call
                        call.state.ringingState.collectLatest {
                            if (it is RingingState.Incoming) {
                                val intent = StreamCallActivity.callIntent(
                                    this@MainActivity,
                                    StreamCallId.fromCallCid(call.cid),
                                    emptyList(),
                                    true,
                                    NotificationHandler.ACTION_INCOMING_CALL,
                                    // use ComposeStreamCallActivity::class.java for default behavior
                                    CustomCallActivity::class.java,
                                    config
                                )
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
}

