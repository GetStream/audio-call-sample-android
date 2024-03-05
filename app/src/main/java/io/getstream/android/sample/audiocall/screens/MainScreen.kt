package io.getstream.android.sample.audiocall.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.android.sample.audiocall.screens.call.DialerScreen
import io.getstream.android.sample.audiocall.videwmodel.MainViewModel.UserUiState
import io.getstream.video.android.compose.theme.VideoTheme


@Composable
fun MainScreen(
    userState: UserUiState,
    onLogin: (userId: String, token: String?) -> Unit = { _, _ -> },
    onLogout: () -> Unit = {},
    onDial: (List<String>) -> Unit = { _ -> },
) {
    when (userState) {
        is UserUiState.Loading -> {
            // Show progress bar
            LoadingScreen()
        }

        is UserUiState.Empty -> {
            // Snow Login
            LoginScreen(onLogin)
        }

        is UserUiState.Actual -> {
            // Show dial screen
            DialerScreen(userState = userState, onLogout, onDial)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    VideoTheme {
        MainScreen(userState = UserUiState.Loading)
    }
}