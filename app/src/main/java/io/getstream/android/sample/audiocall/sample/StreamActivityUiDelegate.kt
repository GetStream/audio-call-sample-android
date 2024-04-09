package io.getstream.android.sample.audiocall.sample

import io.getstream.video.android.core.Call

interface StreamActivityUiDelegate<T: StreamCallActivity> {

    /**
     * Set the content for the activity,
     *
     * @param activity the activity
     * @param call the call
     */
    fun setContent(activity: T, call: Call)
}