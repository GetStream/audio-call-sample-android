package io.getstream.android.sample.audiocall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun LoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(VideoTheme.colors.baseSheetPrimary)
    ) {
        CircularProgressIndicator(color = VideoTheme.colors.brandPrimaryLt)
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    VideoTheme {
        LoadingScreen()
    }
}