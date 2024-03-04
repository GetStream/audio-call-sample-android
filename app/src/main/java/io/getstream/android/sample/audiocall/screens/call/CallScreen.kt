package io.getstream.android.sample.audiocall.screens.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.android.sample.audiocall.videwmodel.AudioCallSampleAppVideModel
import io.getstream.android.sample.audiocall.utils.TestData
import io.getstream.android.sample.audiocall.utils.previewCall
import io.getstream.result.Error
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.Call

@Composable
fun AudioCallScreen(
    callUiState: AudioCallSampleAppVideModel.CallUiState,
    onDial: (List<String>) -> Unit,
    onReset: () -> Unit,
    onReject: (Call) -> Unit = {},
    onDecline: (Call) -> Unit = {},
    onCancel: (Call) -> Unit = {},
    onAccept: (Call) -> Unit = {}
) {

    when (callUiState) {
        is AudioCallSampleAppVideModel.CallUiState.Error -> {
            CallFailedScreen(callUiState.err?.message ?: "Call failed to start.", onReset)
        }

        is AudioCallSampleAppVideModel.CallUiState.Established -> {
            AudioCallContent(callUiState.call!!, onReject, onDecline, onCancel, onAccept)
        }

        AudioCallSampleAppVideModel.CallUiState.Undetermined -> {
            DialerScreen(onDial)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CallScreenPreview() {
    AudioCallScreen(callUiState = AudioCallSampleAppVideModel.CallUiState.Undetermined, {

    }, {

    })
}

@Preview(showBackground = true)
@Composable
fun CallScreenErrorPreview() {
    AudioCallScreen(callUiState = AudioCallSampleAppVideModel.CallUiState.Error(Error.GenericError("Error message")),
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
        AudioCallScreen(callUiState = AudioCallSampleAppVideModel.CallUiState.Established(
            previewCall
        ), {

        }, {

        })
    }
}