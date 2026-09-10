package io.getstream.android.sample.audiocall.ui.screens.call

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import kotlinx.coroutines.launch

@Composable
fun LiveHost(
    callId: String,
    client: StreamVideo,
    onBack:()->Unit
) {
    val context = LocalContext.current

    // Step 1 - Update call settings via callConfigRegistry
    client.state.callConfigRegistry.register(
        DefaultCallConfigurations.getLivestreamCallServiceConfig(),
    )

    // Step 2 - join a call, which type is `default` and id is `123`.
    val call = client.call("livestream", callId)

    LaunchCallPermissions(call = call) {
        val result = call.join(create = true)
        result.onError {
            Toast.makeText(context, "uh oh $it", Toast.LENGTH_SHORT).show()
        }
    }
    LiveHostContent(call, onBack)
}

@Composable
private fun LiveHostContent(
    call: Call,
    onBack:()->Unit
) {
    LaunchCallPermissions(call = call)

    val connection by call.state.connection.collectAsState()
    val totalParticipants by call.state.totalParticipants.collectAsState()
    val backstage by call.state.backstage.collectAsState()
    val localParticipant by call.state.localParticipant.collectAsState()
    val video = localParticipant?.video?.collectAsState()?.value
    val duration by call.state.duration.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary)
            .padding(6.dp),
        contentColor = VideoTheme.colors.baseSheetPrimary,
//        backgroundColor = VideoTheme.colors.baseSheetPrimary,
        topBar = {
            if (connection == RealtimeConnection.Connected) {
                if (!backstage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(
                                    color = VideoTheme.colors.brandPrimary,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            text = "Live $totalParticipants",
                            color = Color.White,
                        )

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Live for $duration",
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                } else {
                    Text(
                        text = "Backstage",
                        color = VideoTheme.colors.basePrimary,
                    )
                }
            } else if (connection is RealtimeConnection.Failed) {
                Text(
                    text = "Connection failed",
                    color = VideoTheme.colors.basePrimary,
                )
            }
        },
        bottomBar = {
            val isCameraEnabled by call.camera.isEnabled.collectAsState()

            Row {
                Button(
//                    colors = ButtonDefaults.buttonColors(
//                        contentColor = VideoTheme.colors.brandPrimary,
//                        backgroundColor = VideoTheme.colors.brandPrimary,
//                    ),
                    onClick = {
                        scope.launch {
                            if (backstage) call.goLive() else call.stopLive()
                        }
                    },
                ) {
                    Text(
                        text = if (backstage) "Start Broadcast" else "Stop Broadcast",
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row {
                        ToggleCameraAction(isCameraEnabled = isCameraEnabled) {
                            call.camera.setEnabled(it.isEnabled)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        FlipCameraAction {
                            call.camera.flip()
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        LeaveCallAction {
                            call.leave()
                            onBack()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        ToggleMicrophoneAction(isMicrophoneEnabled = isMicrophoneEnabled) {
                            call.microphone.setEnabled(
                                it.isEnabled,
                            )
                        }
                    }
                }
            }
        },
    ) {
        VideoRenderer(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .clip(RoundedCornerShape(6.dp)),
            call = call,
            video = video,
//            videoRendererConfig = {},
//            videoFallbackContent = {
//                Text(text = "Video rendering failed")
//            },
        )
    }
}
