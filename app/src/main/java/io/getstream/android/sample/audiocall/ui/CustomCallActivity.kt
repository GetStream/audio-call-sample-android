package io.getstream.android.sample.audiocall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

// Override the default ComposeStreamCallActivity
@Suppress("UNCHECKED_CAST")
class CustomCallActivity : ComposeStreamCallActivity() {

    private val _internalDelegate = CustomUiDelegate()

    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity>
        get() = _internalDelegate

    // CustomTextUi is the new delegate that will override the Outgoing call content
    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {

        @Composable
        fun BigColorfulBox(color: Color, text: String) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = TextStyle(color = Color.White, fontSize = 32.sp)
                )
            }
        }

        @Composable
        override fun StreamCallActivity.RejectedContent(call: Call) {
            BigColorfulBox(color = Color.Red, "Rejected")
        }

        @Composable
        override fun StreamCallActivity.NoAnswerContent(call: Call) {
            BigColorfulBox(color = Color.Yellow, "No answer")
        }

        @Composable
        override fun StreamCallActivity.LoadingContent(call: Call) {
            BigColorfulBox(color = Color.Blue, "Loading")
        }

        @Composable
        override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
            if (intent.action == NotificationHandler.ACTION_OUTGOING_CALL) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VideoTheme.colors.baseSheetPrimary)
                ) {
                    io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
                        call = call,
                        isMicrophoneEnabled = false,
                        detailsContent = { members, _ ->
                            ParticipantInformation(
                                isVideoType = false,
                                callStatus = CallStatus.Calling("Disconnected..."),
                                participants = members,
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            ParticipantAvatars(participants = members)
                        },
                        controlsContent = {
                            // Use custom controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                // Close Action
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f)
                                ) {
                                    GenericAction(
                                        enabled = true,
                                        onAction = {
                                            finish()
                                        },
                                        icon = Icons.Default.Close,
                                        color = VideoTheme.colors.baseSheetTertiary,
                                        iconTint = VideoTheme.colors.basePrimary,
                                        modifier = Modifier.size(56.dp) // Defines the size of the IconButton
                                    )
                                    Text(
                                        text = "Close",
                                        color = VideoTheme.colors.basePrimary,
                                        textAlign = TextAlign.Center, // Centers the text horizontally
                                        modifier = Modifier.fillMaxWidth() // Ensures the text is centered below the button
                                    )
                                }

                                // Redial Action
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f)
                                ) {
                                    AcceptCallAction(
                                        onCallAction = {
                                            val restartIntent = intent.apply {
                                                putExtra(
                                                    NotificationHandler.INTENT_EXTRA_CALL_CID,
                                                    StreamCallId(
                                                        "audio_call",
                                                        UUID.randomUUID().toString()
                                                    )
                                                )
                                            }
                                            finish()
                                            startActivity(restartIntent)
                                        },
                                        modifier = Modifier.size(56.dp) // Defines the size of the IconButton
                                    )
                                    Text(
                                        text = "Call again",
                                        color = VideoTheme.colors.basePrimary,
                                        textAlign = TextAlign.Center, // Centers the text horizontally
                                        modifier = Modifier.fillMaxWidth() // Ensures the text is centered below the button
                                    )
                                }
                            }
                        }
                    )
                }
            } else {
                finish()
            }
        }

        @Composable
        override fun StreamCallActivity.CallFailedContent(call: Call, exception: Exception) {
            BigColorfulBox(color = Color.Gray, exception.message ?: "Failed")
        }
    }
}