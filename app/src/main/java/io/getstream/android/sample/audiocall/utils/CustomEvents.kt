@file:OptIn(ExperimentalCoroutinesApi::class)

package io.getstream.android.sample.audiocall.utils

import android.util.Log
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.openapitools.client.models.CustomVideoEvent

const val ALIVE_KEY = "v=fNFzfwLM72c"
const val USER_KEY = "user-key"

/**
 * When ringing call is updated, send I AM ALIVE event.
 */
fun sendImAliveOnRingingCall(scope: CoroutineScope = defaultCoroutineScope()) {
    scope.launch {
        val instance = StreamVideo.instance()
        instance.state.ringingCall.flatMapLatest {
            it?.state?.ringingState ?: flowOf(null)
        }.combine(instance.state.ringingCall) { state, call ->
            Pair(state, call)
        }.collectLatest {
            Log.d("LOGG", "$it")
            if (it.first is RingingState.Incoming) {
                Log.d("LOGG", "Sending alive event")
                it.second?.sendCustomEvent(
                    mapOf(Pair(ALIVE_KEY, true), Pair(USER_KEY, instance.userId))
                )
            }
        }
    }
}

/**
 * Will map any [customEvents] to true/false depending if the "alive"
 * event was received by another user.
 */
fun Call.receiverActive() = customEvents(ALIVE_KEY).map {
        val originatorUser = it.custom.getOrDefault(USER_KEY, null)
        originatorUser != state.me.value?.userId
    }

/**
 * Monitor all custom events.
 */
fun Call.customEvents(withKey: String? = null): Flow<CustomVideoEvent> {
    val events = defaultBufferedFlow<CustomVideoEvent>()
    subscribeFor(CustomVideoEvent::class.java) {
        Log.d("LOGG", "Custom event -> $it")
        val custom = it as CustomVideoEvent
        if (withKey != null) {
            if (custom.custom.containsKey(withKey)) {
                events.tryEmit(custom)
            }
        } else {
            events.tryEmit(it)
        }
    }

    return events
}
