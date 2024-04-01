package io.getstream.android.sample.audiocall.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantAvatars
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantInformation
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import io.getstream.video.android.core.model.CallStatus

@Composable
fun AudioCallContent(
    call: Call,
    onReject: (Call) -> Unit = {},
    onDecline: (Call) -> Unit = {},
    onCancel: (Call) -> Unit = {},
    onAccept: (Call) -> Unit = {},
    onEnd: (Call) -> Unit = {}
) {
    val onCallAction: (CallAction) -> Unit = { callAction ->
        when (callAction) {
            // Microphone or speaker phone
            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
            is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
            // Call actions, we handle nothing here,
            // we just pass the user actions back to the caller composable.
            is LeaveCall -> onReject(call)
            is DeclineCall -> onDecline(call)
            is CancelCall -> onCancel(call)
            is AcceptCall -> onAccept(call)
            else -> Unit
        }
    }

    val ringingState by call.state.ringingState.collectAsStateWithLifecycle()
    val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()

    val controls: @Composable (BoxScope.() -> Unit)? = when (ringingState) {
        is RingingState.Outgoing -> {
            // We must return different controls for the outgoing audio call since
            // default controls show the camera icons.
            { AudioCallControls(isMicrophoneEnabled = micEnabled, onCallAction) }
        }
        // Default controls for everything else
        else -> null
    }

    RingingCallContent(
        modifier = Modifier.background(color = VideoTheme.colors.baseSheetPrimary),
        call = call,
        // Must be set to false so we do not show the camera controls etc..
        isVideoType = false,
        controlsContent = controls,
        onBackPressed = { onReject(call) },
        onRejectedContent = { onReject(call) },
        onNoAnswerContent = { onCancel(call) },
        onCallAction = onCallAction,
        onAcceptedContent = {
            ActiveCallContent(
                call = call, onCallAction = onCallAction
            ) { memberStates, dp ->
                AudioCallDetails(call = call, memberStates = memberStates)
            }
        },
    )
}

@Composable
private fun BoxScope.AudioCallControls(
    isMicrophoneEnabled: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier
                .background(
                    color = VideoTheme.colors.baseSheetPrimary,
                    shape = VideoTheme.shapes.circle,
                )
                .size(VideoTheme.dimens.componentHeightL),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
        )

        Spacer(modifier = Modifier.height(32.dp))

        CancelCallAction(
            modifier = Modifier.size(VideoTheme.dimens.componentHeightL),
            onCallAction = onCallAction,
        )
    }
}

@Composable
private fun ActiveCallContent(
    call: Call, onCallAction: (action: CallAction) -> Unit = { action: CallAction ->
        DefaultOnCallActionHandler.onCallAction(call = call, callAction = action)
    }, details: @Composable (ColumnScope.(List<MemberState>, Dp) -> Unit)? = null
) {
    val micEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    OutgoingCallContent(call = call,
        isVideoType = false,
        detailsContent = details,
        controlsContent = {
            AudioCallControls(micEnabled, onCallAction)
        })
}

@Composable
private fun AudioCallDetails(
    call: Call, memberStates: List<MemberState>
) {
    val duration by call.state.duration.collectAsStateWithLifecycle()
    val durationText = duration?.toString() ?: "In call"
    ParticipantInformation(
        isVideoType = false,
        callStatus = CallStatus.Calling(durationText),
        participants = memberStates,
    )

    Spacer(modifier = Modifier.size(16.dp))
    ParticipantAvatars(participants = memberStates)
}