package io.getstream.android.sample.audiocall.ui

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.getstream.android.sample.audiocall.utils.onReceiverIsActive
import io.getstream.video.android.compose.permission.LaunchPermissionRequest
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.call.controls.actions.AcceptCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.GenericAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleSpeakerphoneAction
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.model.CallStatus
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import kotlinx.coroutines.launch

// Override the default ComposeStreamCallActivity
@Suppress("UNCHECKED_CAST")
class CustomCallActivity : ComposeStreamCallActivity() {

    // Provide a new delegate that shows the UI. This is just a wrapper class that contains
    // @Composable methods.
    override fun <T : StreamCallActivity> uiDelegate(): StreamActivityUiDelegate<T> {
        return CustomUiDelegate() as StreamActivityUiDelegate<T>
    }

    // CustomTextUi is the new delegate that will override the Outgoing call content
    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {
        @Composable
        override fun StreamCallActivity.RootContent(call: Call) {
            VideoTheme {
                LaunchPermissionRequest(listOf(Manifest.permission.RECORD_AUDIO)) {
                    AllPermissionsGranted {
                        // All permissions granted
                        val connection by call.state.connection.collectAsStateWithLifecycle()
                        LaunchedEffect(key1 = connection) {
                            if (connection == RealtimeConnection.Disconnected) {
                                // Redial handling
                            } else if (connection is RealtimeConnection.Failed) {
                                // Safely cast, no need to crash if the error message is missing
                                val conn = connection as? RealtimeConnection.Failed
                                finish()
                            }
                        }

                        RingingCallContent(
                            isVideoType = isVideoCall(call),
                            call = call,
                            modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
                            onBackPressed = {
                                onBackPressed(call)
                            },
                            onOutgoingContent = {
                                    modifier: Modifier,
                                    call: Call,
                                    isVideoType: Boolean,
                                    isShowingHeader: Boolean,
                                    headerContent: @Composable (ColumnScope.() -> Unit)?,
                                    detailsContent: @Composable (
                                    ColumnScope.(
                                        participants: List<MemberState>,
                                        topPadding: Dp,
                                    ) -> Unit
                                    )?,
                                    controlsContent: @Composable (BoxScope.() -> Unit)?,
                                    onBackPressed: () -> Unit,
                                    onCallAction: (CallAction) -> Unit,
                                ->
                                OnOutgoingCallContent(
                                    call = call,
                                    isVideoType = isVideoType,
                                    modifier = modifier,
                                    isShowingHeader = isShowingHeader,
                                    headerContent = headerContent,
                                    detailsContent = detailsContent,
                                    controlsContent = controlsContent,
                                    onBackPressed = onBackPressed,
                                    onCallAction = onCallAction,
                                )
                            },
                            onIncomingContent = {
                                    modifier: Modifier,
                                    call: Call,
                                    isVideoType: Boolean, isShowingHeader: Boolean,
                                    headerContent: @Composable (ColumnScope.() -> Unit)?,
                                    detailsContent: @Composable (
                                    ColumnScope.(
                                        participants: List<MemberState>,
                                        topPadding: Dp,
                                    ) -> Unit
                                    )?,
                                    controlsContent: @Composable (BoxScope.() -> Unit)?,
                                    onBackPressed: () -> Unit,
                                    onCallAction: (CallAction) -> Unit,
                                ->
                                OnIncomingCallContent(
                                    call = call,
                                    isVideoType = isVideoType,
                                    modifier = modifier,
                                    isShowingHeader = isShowingHeader,
                                    headerContent = headerContent,
                                    detailsContent = detailsContent,
                                    controlsContent = controlsContent,
                                    onBackPressed = onBackPressed,
                                    onCallAction = onCallAction,
                                )
                            },
                            onAcceptedContent = {
                                if (isVideoCall(call)) {
                                    DefaultCallContent(call = call)
                                } else {
                                    AudioCallContent(call = call)
                                }
                            },
                            onNoAnswerContent = {
                                NoAnswerContent(call)
                            },
                            onRejectedContent = {
                                RejectedContent(call)
                            },
                            onCallAction = {
                                onCallAction(call, it)
                            },
                        )
                    }

                    SomeGranted { granted, notGranted, showRationale ->
                        // Some of the permissions were granted, you can check which ones.
                        if (showRationale) {
                            NoPermissions(granted, notGranted, true)
                        } else {
                            finish()
                        }
                    }
                    NoneGranted {
                        // None of the permissions were granted.
                        if (it) {
                            NoPermissions(showRationale = true)
                        } else {
                            finish()
                        }
                    }
                }
            }
        }

        @Composable
        override fun OnOutgoingCallContent(
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent: @Composable (ColumnScope.() -> Unit)?,
            detailsContent: @Composable (ColumnScope.(participants: List<MemberState>, topPadding: Dp) -> Unit)?,
            controlsContent: @Composable (BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit
        ) {
            // Define a default text
            var callingText by remember { mutableStateOf("Connecting...") }
            // When receiver is active, update the text
            call.onReceiverIsActive {
                callingText = "Ringing..."
            }
            // Observe participants, required since this OutgoingCallContent does not observe states
            // by itself.
            val participants: List<MemberState> by call.state.members.collectAsStateWithLifecycle()

            // Show outgoing call content using default component.
            OutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                participants = participants,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = { members, _ ->
                    // The details content is the upper part of the screen, containing participant
                    // info and call duration etc..
                    // For the details content, show a column which has:
                    // 1. Avatars
                    // 2. Participant information, basically the text.
                    // In the text use `CallStatus.Calling(text)` with the callingText variable
                    // which will switch its content when the receiver callback is fired.
                    Column(
                        modifier = modifier
                            .background(VideoTheme.colors.baseSheetPrimary)
                            .fillMaxWidth()
                    ) {
                        ParticipantAvatars(participants = members)
                        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))
                        ParticipantInformation(
                            isVideoType = isVideoType,
                            callStatus = CallStatus.Calling(callingText),
                            participants = members,
                        )
                    }
                },
                controlsContent = controlsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }

        @Composable
        override fun StreamCallActivity.AudioCallContent(call: Call) {
            // Override the default content to allow speaker integration
            val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
            val speakerEnabled by call.speaker.isEnabled.collectAsStateWithLifecycle()
            // Use the default AudioCallContent
            io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
                call = call,
                isMicrophoneEnabled = micEnabled,
                controlsContent = {
                    // Use custom controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Use pre-made control components
                        ToggleMicrophoneAction(
                            isMicrophoneEnabled = micEnabled,
                            onCallAction = {
                                onCallAction(call, it)
                            },
                            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
                            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle(),
                        )

                        ToggleSpeakerphoneAction(
                            isSpeakerphoneEnabled = speakerEnabled,
                            onCallAction = {
                                onCallAction(call, it)
                            },
                        )

                        CancelCallAction(
                            onCallAction = {
                                onCallAction(call, it)
                            },
                        )
                    }

                    Spacer(modifier = Modifier.size(16.dp))
                }
            )
        }

        @Composable
        override fun StreamCallActivity.NoAnswerContent(call: Call) {
            // Override the default content to allow speaker integration
            val participants by call.state.members.collectAsStateWithLifecycle()
            val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
            val speakerEnabled by call.speaker.isEnabled.collectAsStateWithLifecycle()
            // Use the default AudioCallContent to mimic the same screen
            io.getstream.video.android.compose.ui.components.call.activecall.AudioCallContent(
                call = call,
                isMicrophoneEnabled = micEnabled,
                detailsContent = { _, _ ->
                    Column(
                        modifier = Modifier
                            .background(VideoTheme.colors.baseSheetPrimary)
                            .fillMaxWidth()
                    ) {
                        ParticipantAvatars(participants = participants)
                        Spacer(modifier = Modifier.height(VideoTheme.dimens.spacingM))
                        ParticipantInformation(
                            isVideoType = isVideoCall(call),
                            callStatus = CallStatus.Calling("No answer"),
                            participants = participants,
                        )
                    }
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
                        // Use pre-made control components
                        Column {

                            GenericAction(
                                modifier = Modifier,
                                enabled = true,
                                onAction = {
                                    finish()
                                },
                                icon = Icons.Default.Close,
                                color = VideoTheme.colors.baseSheetTertiary,
                                iconTint = VideoTheme.colors.basePrimary,
                            )
                            Text(text = "Close", color = VideoTheme.colors.basePrimary)
                        }

                        // Redial
                        Column {
                            AcceptCallAction(
                                onCallAction = {
                                    lifecycleScope.launch {
                                        // Ring again
                                        val intent = callIntent(
                                            this@NoAnswerContent,
                                            StreamCallId.fromCallCid(call.cid),
                                            participants.map {
                                                it.user.id
                                            },
                                            true,
                                            NotificationHandler.ACTION_OUTGOING_CALL,
                                            CustomCallActivity::class.java
                                        )
                                        startActivity(intent)
                                        finish()
                                    }
                                },
                            )
                            Text(text = "Call again", color = VideoTheme.colors.basePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}