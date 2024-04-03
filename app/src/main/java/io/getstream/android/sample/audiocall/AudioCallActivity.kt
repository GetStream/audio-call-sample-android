package io.getstream.android.sample.audiocall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import io.getstream.android.sample.audiocall.videwmodel.CallViewModel
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.onErrorSuspend
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.VideoEvent
import java.util.UUID
import kotlin.IllegalStateException

abstract class AudioCallActivity : DefaultCallActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?,
        call: Call
    ) {
        super.onCreate(savedInstanceState, persistentState, call)

    }
}