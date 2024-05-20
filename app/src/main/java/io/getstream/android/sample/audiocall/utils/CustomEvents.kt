@file:OptIn(ExperimentalCoroutinesApi::class)

package io.getstream.android.sample.audiocall.utils

import android.util.Log
import io.getstream.android.sample.audiocall.AudioCallSampleApp
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.openapitools.client.models.CustomVideoEvent

const val ALIVE_KEY = "v=fNFzfwLM72c"
const val BUSY_KEY = "v=weoiyha213781"
const val USER_KEY = "user-key"

// Utilities
/**
 * Creates a default coroutine scope for monitoring events on IO thread.
 */
fun defaultCoroutineScope(baseScope: CoroutineScope = AudioCallSampleApp.applicationScope) =
    baseScope + Dispatchers.IO

/**
 * Create a default buffered flow.
 */
fun <T> defaultBufferedFlow() = MutableSharedFlow<T>(
    replay = 0,
    extraBufferCapacity = 15, // 15 events to be buffered at most
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

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
            if (it.first is RingingState.Incoming) {
                it.second?.sendCustomEvent(
                    mapOf(Pair(ALIVE_KEY, true), Pair(USER_KEY, instance.userId))
                )
            }
        }
    }
}

/**
 * When ringing call is updated, send I AM ALIVE event.
 */
fun rejectCallsFromTheSameUser(scope: CoroutineScope = defaultCoroutineScope()) {
    scope.launch {
        val instance = StreamVideo.instance()
        combine(
            instance.state.ringingCall.flatMapLatest {
                it?.state?.ringingState ?: flowOf(null)
            }, instance.state.ringingCall, instance.state.activeCall
        ) { state, ringing, active ->
            Triple(state, ringing, active)
        }.collect {
            if (it.first is RingingState.Active) {
                // Check active call.
                val active = it.third
                Log.d("BUSY", "ringing: ${it.second?.cid}, active: ${it.third?.cid}")
                if (active != null) {
                    val caller = it.second?.state?.members?.value?.filterNot { member ->
                        member.user.id == instance.userId
                    }?.first()
                    val currentCallCaller = active.state.members.value.filterNot { member ->
                        member.user.id == instance.userId
                    }.first()
                    if (caller?.user?.id == currentCallCaller.user.id) {
                        it.second?.sendCustomEvent(
                            mapOf(Pair(BUSY_KEY, true), Pair(USER_KEY, instance.userId))
                        )
                        // Will reject the incoming call.
                        it.second?.reject()
                    }
                }
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
