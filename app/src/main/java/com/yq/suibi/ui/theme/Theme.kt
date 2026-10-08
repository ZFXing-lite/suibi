package com.yq.suibi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SuibiTheme(
    palette: SuibiPalette,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = palette.scheme(),
        typography = suibiTypography(fontScale),
        content = content
    )
}