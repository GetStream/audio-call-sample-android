package io.getstream.android.sample.audiocall.screens.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.android.sample.audiocall.utils.TestData
import io.getstream.android.sample.audiocall.utils.previewCall
import io.getstream.android.sample.audiocall.videwmodel.CallViewModel.CallUiState
import io.getstream.result.Error
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call

@Composable
fun AudioCallScreen(
    callUiState: CallUiState,
    onReset: () -> Unit = {},
    onReject: (Call) -> Unit = {},
    onDecline: (Call) -> Unit = {},
    onCancel: (Call) -> Unit = {},
    onAccept: (Call) -> Unit = {},
    onEnd: (Call) -> Unit = {}
) {
    when (callUiState) {
        is CallUiState.Error -> {
            CallFailedScreen(callUiState.err?.message ?: "Call failed to start.", onReset)
        }

        is CallUiState.Established -> {
            AudioCallContent(callUiState.call!!, onReject, onDecline, onCancel, onAccept, onEnd)
        }

        is CallUiState.Ended -> {
            // Reset the UI
            onReset()
        }

        is CallUiState.Undetermined -> {
            // Do nothing, we are waiting for the call to start.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallScreenPreview() {
    AudioCallScreen(
        callUiState = CallUiState.Undetermined,
        {

        },
        {

        })
}

@Preview(showBackground = true)
@Composable
fun CallScreenErrorPreview() {
    AudioCallScreen(
        callUiState = CallUiState.Error(Error.GenericError("Error message")),
        {

        },
        {

        })
}

@Preview(showBackground = true)
@Composable
fun CallScreenContentPreview() {
    val context = LocalContext.current
    TestData.initializeStreamVideo(context)
    VideoTheme {
        AudioCallScreen(
            callUiState = CallUiState.Established(
                previewCall
            ),
            {

            },
            {

            })
    }
}