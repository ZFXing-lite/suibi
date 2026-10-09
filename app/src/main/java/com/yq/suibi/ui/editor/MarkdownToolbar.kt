package com.yq.suibi.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 工具栏按钮当前生效状态，决定哪个按钮高亮。 */
data class MdState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val heading: Boolean = false,
    val bullet: Boolean = false,
    val ordered: Boolean = false,
    val quote: Boolean = false
)

private data class MdButton(
    val label: String,
    val mono: Boolean = false,
    val bold: Boolean = false,
    val on: Boolean = false,
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
    modifier: Modifier = Modifier,
    active: MdState = MdState()
) {
    val buttons = listOf(
        MdButton("B", bold = true, on = active.bold, onClick = onBold),
        MdButton("I", mono = true, on = active.italic, onClick = onItalic),
        MdButton("H", bold = true, on = active.heading, onClick = onHeading),
        MdButton("•", on = active.bullet, onClick = onBullet),
        MdButton("1.", on = active.ordered, onClick = onOrdered),
        MdButton("\u201C", on = active.quote, onClick = onQuote),
        MdButton("<>", mono = true, on = active.code, onClick = onCode),
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
                    // 生效中的按钮加个底色块，一眼看得出当前是粗体还是斜体
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (b.on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                    ) {
                        Text(
                            text = b.label,
                            fontSize = 15.sp,
                            fontWeight = if (b.bold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = if (b.mono) FontFamily.Monospace else FontFamily.Default,
                            color = if (b.on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}