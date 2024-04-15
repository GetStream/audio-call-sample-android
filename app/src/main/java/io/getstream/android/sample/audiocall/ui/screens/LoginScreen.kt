package io.getstream.android.sample.audiocall.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.getstream.video.android.compose.theme.VideoTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnsureAudioPermission() {
    // While the SDK will handle the microphone permission,
    // its not a bad idea to do it prior to entering any call UIs
    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            // Access to microphone
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Allow for foreground service for notification on API 26+
                add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }
    )

    LaunchedEffect(key1 = true) {
        permissionsState.launchMultiplePermissionRequest()
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    // States for user ID and token
    var userId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    // Ensure audio permission
    if (!LocalInspectionMode.current) {
        EnsureAudioPermission()
    }

    // Centered Box
    Box(
        contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
    ) {
        // Content Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            TextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token (Optional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = userId.isNotBlank(),
                onClick = { onLogin(userId, token) }, modifier = Modifier.align(Alignment.End)
            ) {
                Text("Login")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    VideoTheme {
        LoginScreen { _, _ ->

        }
    }
}