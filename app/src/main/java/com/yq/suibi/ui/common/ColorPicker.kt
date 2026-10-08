package com.yq.suibi.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yq.suibi.ui.theme.MarkColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** HSV → RGB。h ∈ [0,360)，s、v ∈ [0,1]。 */
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val hh = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs(hh % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        hh < 1f -> Triple(c, x, 0f)
        hh < 2f -> Triple(x, c, 0f)
        hh < 3f -> Triple(0f, c, x)
        hh < 4f -> Triple(0f, x, c)
        hh < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, 1f)
}

/** RGB → HSV。 */
private fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val s = if (mx == 0f) 0f else d / mx
    return Triple(h, s, mx)
}

/**
 * 标记颜色选择器：预设色 + 自定义调色盘（明度/饱和度方块 + 色相条）。
 */
@Composable
fun MarkPickerDialog(
    current: Int?,
    onDismiss: () -> Unit,
    onPick: (Int?) -> Unit
) {
    val start = current?.let { Color(it) }
    val initialHsv = start?.let { colorToHsv(it) } ?: Triple(28f, 0.55f, 0.75f)

    var hue by remember { mutableFloatStateOf(initialHsv.first) }
    var sat by remember { mutableFloatStateOf(initialHsv.second) }
    var value by remember { mutableFloatStateOf(initialHsv.third) }
    var picked by remember { mutableStateOf(start) }

    val custom = hsvToColor(hue, sat, value)

    fun syncFrom(c: Color) {
        val (h, s, v) = colorToHsv(c)
        hue = h
        sat = s
        value = v
        picked = c
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "标记颜色",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(14.dp))

                // 预设色
                for (row in MarkColors.chunked(7)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c ->
                            val selected = picked != null && picked!!.toArgb() == c.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { syncFrom(c) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    text = "自定义",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // 明度 / 饱和度方块
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(hue) {
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val ch = e.changes.firstOrNull() ?: continue
                                    if (ch.pressed) {
                                        sat = (ch.position.x / size.width).coerceIn(0f, 1f)
                                        value = (1f - ch.position.y / size.height).coerceIn(0f, 1f)
                                        picked = hsvToColor(hue, sat, value)
                                        ch.consume()
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(hsvToColor(hue, 1f, 1f))
                        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    }
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (sat * 130f).dp - 9.dp,
                                y = ((1f - value) * 130f).dp - 9.dp
                            )
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .background(custom.copy(alpha = 0.25f))
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 色相条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val ch = e.changes.firstOrNull() ?: continue
                                    if (ch.pressed) {
                                        hue = (ch.position.x / size.width).coerceIn(0f, 1f) * 360f
                                        picked = hsvToColor(hue, sat, value)
                                        ch.consume()
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(
                            Brush.horizontalGradient(
                                (0..12).map { hsvToColor(it * 30f, 1f, 1f) }
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = (hue / 360f * 260f).dp - 3.dp)
                            .width(6.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                picked ?: MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = picked?.let {
                            "#%06X".format(it.toArgb() and 0xFFFFFF)
                        } ?: "未标记",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onPick(null); onDismiss() }) {
                        Text("清除标记")
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        onPick(picked?.toArgb())
                        onDismiss()
                    }) { Text("确定") }
                }
            }
        }
    }
}