package io.getstream.android.sample.audiocall.utils

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.openapitools.client.models.VideoEvent

// Utilities
/**
 * Creates a default coroutine scope.
 */
fun defaultCoroutineScope() = MainScope() + Dispatchers.IO

/**
 * Create a default buffered flow.
 */
fun <T> defaultBufferedFlow() = MutableSharedFlow<T>(
    replay = 0,
    extraBufferCapacity = 15, // 15 events to be buffered at most
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

/**
 * Convenience logger
 */
object Logging {
    val log by taggedLogger("CallEventsObserver")
}

/**
 * Where does the update come from.
 */
enum class CallOrigin {
    /**
     * Ringing call is not null and the update is from there.
     */
    RINGING,

    /**
     * Active call is not null and the event update is from the active call.
     */
    ACTIVE,

    /**
     * Video event received, but both active and ringing calls are null (can happen in health-check events etc..)
     */
    NONE,

    /**
     * We are monitoring events for a single call with known ID.
     */
    STANDALONE,

    /**
     * Update came in, but both active and ringing call are not null (they are the same reference thou, always)
     */
    BOTH,

    /**
     * This update was for the stream client and not a specific call.
     */
    CLIENT,
}

/**
 * A single update containing most of the needed information for the SDK state.
 */
data class CallStateUpdate(
    val origin: CallOrigin = CallOrigin.RINGING,
    val call: Call? = null,
    val event: VideoEvent? = null,
    val ringingState: RingingState? = null,
    val connectionState: RealtimeConnection? = null,
    val participantCount: Int = 0
)

/**
 * Observer for single call ID.
 */
fun callEvents(scope: CoroutineScope = defaultCoroutineScope(), cid: StreamCallId): Flow<CallStateUpdate> = channelFlow {
    val instance = StreamVideo.instance()
    val call = instance.call(cid.type, cid.id)
    val job = singleCallEventsMonitor(call, scope)

    awaitClose {
        job.cancel()
    }
}

/**
 * Observe `activeCall` and `ringingCall` along with call events and states.
 */
fun callEvents(scope: CoroutineScope = defaultCoroutineScope()): Flow<CallStateUpdate> = channelFlow {
    val job = scope.launch {
        // Stream video instance
        val instance = StreamVideo.instance()

        // Instance level events
        instance.subscribe {
            trySend(
                CallStateUpdate(
                    CallOrigin.CLIENT,
                    null,
                    it,
                    null,
                    null,
                    0

                )
            )
        }

        // Active and ringing flows
        val activeCallFlow = instance.state.activeCall
        val ringingCallFlow = instance.state.ringingCall

        // Combine active and ringing flows
        val callFlow = combine(activeCallFlow, ringingCallFlow) { active, ringing ->
            if (active != null && ringing == null) {
                Pair(active, CallOrigin.ACTIVE)
            } else if (ringing != null && active == null) {
                Pair(ringing, CallOrigin.RINGING)
            } else if (ringing != null) {
                Pair(active, CallOrigin.BOTH)
            } else {
                Pair(null, CallOrigin.NONE)
            }
        }


        var callLevelCollector: Job? = null
        callFlow.collectLatest { pair ->
            val call = pair.first
            if (call != null) {
                callLevelCollector?.cancel()
                callLevelCollector = singleCallEventsMonitor(call, scope)
            } else {
                send(CallStateUpdate(origin = pair.second)) // empty
            }
        }
    }

    awaitClose {
        Logging.log.d { "Closing observers, channel closed." }
        job.cancel()
    }
}

/**
 * Get call events from `subscribe()` as a flow.
 */
fun Call.events(): Flow<VideoEvent> {
    val events = defaultBufferedFlow<VideoEvent>()
    subscribe {
        events.tryEmit(it)
    }
    return events
}

private fun ProducerScope<CallStateUpdate>.singleCallEventsMonitor(
    call: Call,
    scope: CoroutineScope
): Job {
    val ringingStateFlow = call.state.ringingState
    val connectionFlow = call.state.connection
    val participantCount = call.state.participantCounts
    val events = call.events()

    val callStateFlow = combine(
        ringingStateFlow,
        connectionFlow,
        participantCount,
        events
    ) { ringingState, connection, count, event ->
        CallStateUpdate(
            CallOrigin.STANDALONE,
            call,
            event,
            ringingState,
            connection,
            count?.total ?: 0
        )
    }

    val job = scope.launch {
        callStateFlow.collectLatest {
            send(it)
        }
    }
    return job
}