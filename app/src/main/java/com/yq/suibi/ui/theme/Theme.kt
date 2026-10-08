package com.yq.suibi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SuibiTheme(
    palette: SuibiPalette,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = palette.scheme(),
        typography = SuibiTypography,
        content = content
    )
}