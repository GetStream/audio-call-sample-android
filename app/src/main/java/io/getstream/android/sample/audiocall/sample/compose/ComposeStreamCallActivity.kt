package io.getstream.android.sample.audiocall.sample.compose

import io.getstream.android.sample.audiocall.sample.StreamActivityUiDelegate
import io.getstream.android.sample.audiocall.sample.StreamCallActivity


open class ComposeStreamCallActivity : StreamCallActivity() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : StreamCallActivity> uiDelegate(): StreamActivityUiDelegate<T> {
        return StreamCallActivityComposeDelegate() as StreamActivityUiDelegate<T>
    }
}