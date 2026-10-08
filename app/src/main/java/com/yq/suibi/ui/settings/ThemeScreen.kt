package com.yq.suibi.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yq.suibi.data.SettingsStore
import com.yq.suibi.ui.common.SI
import com.yq.suibi.ui.theme.SuibiPalette
import com.yq.suibi.ui.theme.Palettes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    onPaletteChange: (String) -> Unit
) {
    val currentId by store.paletteId.collectAsStateWithLifecycle(initialValue = "xuanzhi")
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("主题") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(SI.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(Palettes.all, key = { it.id }) { palette ->
                PaletteCard(
                    palette = palette,
                    selected = palette.id == currentId,
                    onClick = {
                        scope.launch { store.setPalette(palette.id) }
                        onPaletteChange(palette.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: SuibiPalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = palette.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.primary else palette.outline,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // 迷你预览：底色 + 卡片 + 主色
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.background)
                    .border(1.dp, palette.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp, 24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.outline, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp, top = 5.dp)
                            .size(12.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.primary)
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp, top = 12.dp)
                            .size(20.dp, 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(palette.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = palette.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = palette.author + if (palette.dark) " · 深色" else " · 浅色",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant
                )
            }

            // 主色点
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(palette.primary)
            )

            if (selected) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    SI.Check,
                    contentDescription = "已选",
                    tint = palette.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}