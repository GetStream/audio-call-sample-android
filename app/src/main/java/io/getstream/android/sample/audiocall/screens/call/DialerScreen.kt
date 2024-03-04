package io.getstream.android.sample.audiocall.screens.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun DialerScreen(onDial: (List<String>) -> Unit) {
    var userIdsInput by remember { mutableStateOf("") }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
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
            Button(
                enabled = userIdsInput.isNotBlank(),
                onClick = {
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

@Preview(showBackground = true)
@Composable
private fun DialerPreview() {
    VideoTheme {
        DialerScreen {

        }
    }
}