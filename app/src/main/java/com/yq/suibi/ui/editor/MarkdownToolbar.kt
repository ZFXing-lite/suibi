package com.yq.suibi.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MdButton(
    val label: String,
    val mono: Boolean = false,
    val bold: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun MarkdownToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeading: () -> Unit,
    onBullet: () -> Unit,
    onOrdered: () -> Unit,
    onQuote: () -> Unit,
    onCode: () -> Unit,
    onDivider: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttons = listOf(
        MdButton("B", bold = true, onClick = onBold),
        MdButton("I", mono = true, onClick = onItalic),
        MdButton("H", bold = true, onClick = onHeading),
        MdButton("•", onClick = onBullet),
        MdButton("1.", onClick = onOrdered),
        MdButton("\u201C", onClick = onQuote),
        MdButton("<>", mono = true, onClick = onCode),
        MdButton("—", onClick = onDivider)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 2.dp)
        ) {
            buttons.forEach { b ->
                TextButton(
                    onClick = b.onClick,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = b.label,
                        fontSize = 15.sp,
                        fontWeight = if (b.bold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = if (b.mono) FontFamily.Monospace else FontFamily.Default,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}