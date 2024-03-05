package io.getstream.android.sample.audiocall

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.getstream.android.sample.audiocall.screens.call.AudioCallScreen
import io.getstream.android.sample.audiocall.videwmodel.CallViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.NotificationHandler.Companion.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.streamCallId

class CallActivity : ComponentActivity() {
    // This is just the simplest and fastest way to create the view model without any dependencies
    // In a real app you should utilize a different method of creating the view model.
    private val viewModel: CallViewModel = CallViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.appBackground
                ) {
                    // Data to work with
                    val action = intent.action
                    val callInfo = intent.streamCallId(INTENT_EXTRA_CALL_CID)
                    // Check if we have call info in the intent (assuming incoming call)
                    if (callInfo != null) {
                        // We have an action triggered with call ID,
                        // we must determine what the action was.
                        when (action) {
                            NotificationHandler.ACTION_ACCEPT_CALL -> {
                                viewModel.accept(callInfo)
                            }

                            NotificationHandler.ACTION_REJECT_CALL -> {
                                viewModel.reject(callInfo)
                            }

                            NotificationHandler.ACTION_INCOMING_CALL -> {
                                viewModel.incoming(callInfo)
                            }

                            OUTGOING_CALL_ACTION -> {
                                // Extract the members and the call ID and place the outgoing call
                                val members = intent.getStringArrayListExtra(EXTRA_MEMBERS_ARRAY)
                                viewModel.call(callInfo, members = members!!)
                            }
                        }
                    }

                    AudioCallScreen(callUiState = viewModel.callUiState, onReset = {
                        finish()
                    }, onReject = {
                        viewModel.reject(it)
                    }, onAccept = {
                        viewModel.accept(it)
                    }, onDecline = {
                        viewModel.reject(it)
                    }, onCancel = {
                        viewModel.cancel(it)
                    })
                }
            }
        }
    }

    override fun finish() {
        // Cleanup any calls we may have since our call UI is gone.
        // This should be done elsewhere if state is managed differently
        // for the sample app we just do it here.
        StreamVideo.instance().state.activeCall.value?.leave()
        super.finish()
    }

    companion object {
        const val EXTRA_MEMBERS_ARRAY: String = "members_extra"
        const val OUTGOING_CALL_ACTION = "OUTGOING_CALL"
        fun placeCallIntent(context: Context, cid: StreamCallId, members: List<String>): Intent {
            return Intent(context, CallActivity::class.java).apply {
                // Setup the outgoing call action
                action = OUTGOING_CALL_ACTION
                // Add the generated call ID
                putExtra(INTENT_EXTRA_CALL_CID, cid)
                // Setup the members to transfer to the new activity
                val membersArrayList = ArrayList<String>()
                members.forEach { membersArrayList.add(it) }
                putStringArrayListExtra(EXTRA_MEMBERS_ARRAY, membersArrayList)
            }
        }
    }
}