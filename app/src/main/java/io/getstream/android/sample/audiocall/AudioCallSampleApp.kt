package io.getstream.android.sample.audiocall

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import com.google.firebase.FirebaseApp
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.android.sample.audiocall.auth.Client
import io.getstream.android.sample.audiocall.notifications.NotificationService
import io.getstream.android.sample.audiocall.notifications.RejectBusyNotificationHandler
import io.getstream.android.sample.audiocall.storage.UserData
import io.getstream.android.sample.audiocall.storage.UserStorage
import io.getstream.android.sample.audiocall.utils.rejectCallsFromTheSameUser
import io.getstream.android.sample.audiocall.utils.sendImAliveOnRingingCall
import io.getstream.log.Priority
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.HttpLoggingLevel
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class AudioCallSampleApp : Application() {

    val logger by taggedLogger("AudioCallApp")

    companion object {
        lateinit var instance: AudioCallSampleApp
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize firebase first.
        // Ensure that you have the correct service account credentials updated in the Stream Dashboard.
        FirebaseApp.initializeApp(this)

        // You do not have to use runBlocking { } to initialize the StreamVideo instance. Its only for the purpose of loading the mock user.
        runBlocking {
            val userData = UserStorage.loadUser(instance)
            if (userData !is UserData.NoUser) {
                // We have a logged in user, we are going to initialize the SDK.
                streamVideo(userData)
                sendImAliveOnRingingCall()
                rejectCallsFromTheSameUser()
            }
        }
    }


    /**
     * Get the [StreamVideo] instance to be used further with the app.
     */
    fun streamVideo(userData: UserData): StreamVideo {
        val sdkInstance = StreamVideo.instanceOrNull()
        // Stream video may have an instance already
        // 1. If there is an instance check if its for the correct user, if yes, return the instance
        // 2. If there is no instance or its not for the correct user, create new instance and init
        // the SDK from start.
        val preparedInstance = if (sdkInstance == null || sdkInstance.userId != userData.userId) {
            // If there is already an instance, just return it
            // otherwise build a new StreamVideo instance
            val userId = userData.userId
            val token = userData.token
            // We simulate an Authenticated user
            val user = User(
                id = userId, name = userId.capitalize(Locale.current), type = UserType.Authenticated
            )
            val builder = StreamVideoBuilder(
                // Only for the purpose of debugging and logging
                loggingLevel = LoggingLevel(Priority.VERBOSE, HttpLoggingLevel.BODY),
                context = applicationContext,
                // Make sure to change to your API key, found in the Stream Dashboard.
                apiKey = "k436tyde94hj",
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = token,
                crashOnMissingPermission = false,
                permissionCheck = object : StreamPermissionCheck {
                    override fun checkAndroidPermissions(context: Context, call: Call): Boolean {
                        // Pass for all permissions.
                        return true
                    }

                },
                tokenProvider = {
                    logger.d { "[tokenProvider] Refreshing the token." }
                    provideToken(userId)
                },
                notificationConfig = NotificationConfig(
                    // Custom  notification handler
                    notificationHandler = RejectBusyNotificationHandler(this),
                    // Make the notification low prio if the app is in foreground, so its not visible as a popup, since we want to handle
                    // the incoming call in full screen when app is running.
                    hideRingingNotificationInForeground = true,
                    // Make sure that the provider name is equal to the "Name" of the configuration in Stream Dashboard.
                    pushDeviceGenerators = listOf(FirebasePushDeviceGenerator(providerName = NotificationService.FIREBASE_CONFIG_NAME_ON_DASHBOARD))
                ),
            )
            // Build a new instance
            builder.build()
        } else {
            // Just return the existing instance
            sdkInstance
        }

        return preparedInstance
    }

    private suspend fun provideToken(userId: String): String {
        // If your tokens are expiring they can be updated here.
        val newToken = expirableToken(userId)
        UserStorage.updateToken(instance, newToken)
        return newToken
    }
}



suspend fun expirableToken(userId: String): String {
    require(userId.isNotEmpty()) { "User id must not be empty" }
    return Client.getToken(userId).token
}