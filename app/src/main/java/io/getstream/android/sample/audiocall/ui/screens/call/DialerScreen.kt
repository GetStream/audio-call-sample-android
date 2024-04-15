package io.getstream.android.sample.audiocall.ui.screens.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.android.sample.audiocall.videwmodel.MainViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar

/**
 * The dial screen, where we can input user IDs to call other members
 */
@Composable
fun DialerScreen(
    userState: MainViewModel.UserUiState,
    onLogout: () -> Unit = {},
    onDial: (List<String>) -> Unit
) {

    var userIdsInput by remember { mutableStateOf("") }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        UserAndLogoutRow(userState, onLogout)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = userIdsInput,
                onValueChange = { userIdsInput = it },
                label = { Text(text = "Enter User IDs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(enabled = userIdsInput.isNotBlank(), onClick = {
                // Assuming user IDs are separated by commas, spaces, or semicolons
                val userIds = userIdsInput.split(",", ";", " ").filter { it.isNotBlank() }
                onDial(userIds)
            }) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "Dial")
                Spacer(modifier = Modifier.height(20.dp))
                Text("Dial")
            }
        }
    }
}

/**
 * Show user data and logout button.
 */
@Composable
private fun BoxScope.UserAndLogoutRow(
    userState: MainViewModel.UserUiState,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.Companion
            .align(Alignment.TopCenter)
            .fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val actual = userState as? MainViewModel.UserUiState.Actual
        if (actual != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    modifier = Modifier.size(44.dp),
                    userName = actual.streamUser.id,
                    userImage = actual.streamUser.image
                )
                Spacer(modifier = Modifier.size(16.dp))
                Text(text = userState.userData.userId)
            }

            Button(onClick = onLogout) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout")
                Text(text = "Logout")
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun DialerPreview() {
    VideoTheme {
        DialerScreen(MainViewModel.UserUiState.Empty) {

        }
    }
}