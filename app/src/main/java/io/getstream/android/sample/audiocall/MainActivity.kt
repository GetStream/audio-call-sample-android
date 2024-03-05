package io.getstream.android.sample.audiocall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.getstream.android.sample.audiocall.screens.MainScreen
import io.getstream.android.sample.audiocall.videwmodel.MainViewModel
import io.getstream.video.android.compose.theme.VideoTheme

class MainActivity : ComponentActivity() {
    // This is just the simplest and fastest way to create the view model without any dependencies
    // In a real app you should utilize a different method of creating the view model.
    private val viewModel: MainViewModel =
        MainViewModel(AudioCallSampleApp.instance)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = VideoTheme.colors.appBackground
                ) {
                    val context = LocalContext.current
                    val userUiState = viewModel.userState
                    MainScreen(
                        userState = userUiState,
                        onLogin = { userId, token ->
                            viewModel.login(context = context, userId = userId, token = token)
                        },
                        onLogout = {
                            viewModel.logout(context = context)
                        },
                        onDial = { members ->
                            viewModel.placeCall(context = context, members = members)
                        })
                }
            }
        }
    }
}