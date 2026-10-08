package com.yq.suibi.ui.common

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 左滑露出操作按钮的行容器。
 *
 * 手势只在水平方向成立，纵向滚动不受影响。
 * [open] 由列表统一持有，保证同时只有一行展开。
 */
@Composable
fun SwipeRevealRow(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actionsWidth: Dp,
    cornerRadius: Dp,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { actionsWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(open, widthPx) {
        val target = if (open) -widthPx else 0f
        if (offset != target) {
            animate(
                initialValue = offset,
                targetValue = target,
                animationSpec = tween(170)
            ) { value: Float, _: Float -> offset = value }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(cornerRadius)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(-widthPx, 0f)
                    },
                    onDragStopped = { velocity ->
                        val shouldOpen = offset < -widthPx * 0.45f || velocity < -900f
                        onOpenChange(shouldOpen)
                        scope.launch {
                            val target = if (shouldOpen) -widthPx else 0f
                            animate(
                                initialValue = offset,
                                targetValue = target,
                                animationSpec = tween(170)
                            ) { value: Float, _: Float -> offset = value }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

/** 左滑露出的单个操作块。 */
@Composable
fun RowScope.SwipeAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(74.dp)
            .background(tint)
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}