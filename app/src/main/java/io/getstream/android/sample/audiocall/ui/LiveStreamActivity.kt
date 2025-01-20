package io.getstream.android.sample.audiocall.ui

import androidx.compose.runtime.Composable
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity

class LiveStreamActivity : ComposeStreamCallActivity() {

    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity> = CustomUiDelegate()

    private class CustomUiDelegate : StreamCallActivityComposeDelegate() {
        @Composable
        override fun StreamCallActivity.RootContent(call: Call) {
            VideoTheme {
                LivestreamPlayer(call = call)
            }
        }
    }
}