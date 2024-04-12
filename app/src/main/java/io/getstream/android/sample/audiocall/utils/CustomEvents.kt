package io.getstream.android.sample.audiocall.utils

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.openapitools.client.models.CustomVideoEvent

const val ALIVE_KEY = "v=fNFzfwLM72c"

/**
 * Monitor the [ALIVE_KEY] and custom events. If they arrive fire the callback.
 */
fun Call.onReceiverIsActive(scope: CoroutineScope = defaultCoroutineScope(), callback: () -> Unit) {
    scope.launch(Dispatchers.IO) {
        this@onReceiverIsActive.customEvents().collectLatest {
            if (it.custom.containsKey(ALIVE_KEY)) {
                callback() // Invoke callback.
            }
        }
    }
}

/**
 * When rigning call is updated, send I AM ALIVE event.
 */
fun sendImAliveOnRingingCall(scope: CoroutineScope = defaultCoroutineScope()) {
    scope.launch {
        val instance = StreamVideo.instance()
        instance.state.ringingCall.collectLatest {
            it?.sendCustomEvent(
                mapOf(Pair(ALIVE_KEY, true))
            )
        }
    }
}

/**
 * Monitor all custom events.
 */
fun Call.customEvents(): Flow<CustomVideoEvent> {
    val events = defaultBufferedFlow<CustomVideoEvent>()
    subscribeFor(CustomVideoEvent::class.java) {
        events.tryEmit(it as CustomVideoEvent)
    }

    return events
}
