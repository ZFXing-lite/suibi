package com.yq.suibi.ui.topics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.Topic
import com.yq.suibi.data.TopicStats
import com.yq.suibi.ui.common.ActionSheet
import com.yq.suibi.ui.common.ConfirmDialog
import com.yq.suibi.ui.common.DeleteConfirmDialog
import com.yq.suibi.ui.common.DeleteIcon
import com.yq.suibi.ui.common.EmptyState
import com.yq.suibi.ui.common.MarkPickerDialog
import com.yq.suibi.ui.common.RenameIcon
import com.yq.suibi.ui.common.SI
import com.yq.suibi.ui.common.SheetAction
import com.yq.suibi.ui.common.SwipeAction
import com.yq.suibi.ui.common.SwipeRevealRow
import com.yq.suibi.ui.common.TextInputDialog
import com.yq.suibi.ui.common.relativeTime
import com.yq.suibi.ui.common.timeText
import com.yq.suibi.ui.common.vmFactory
import com.yq.suibi.ui.theme.tintSurface

private val RowShape = RoundedCornerShape(16.dp)
private val ActionsWidth = 74.dp * 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicListScreen(
    db: AppDatabase,
    onOpenTopic: (Long) -> Unit,
    onOpenAllNotes: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewNote: (Long) -> Unit
) {
    val vm: TopicListViewModel = viewModel(factory = vmFactory { TopicListViewModel(db) })
    val topics by vm.topics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val app = context.applicationContext as com.yq.suibi.SuibiApp
    val general by app.settings.general.collectAsStateWithLifecycle(
        initialValue = com.yq.suibi.data.GeneralConfig()
    )

    var showCreate by remember { mutableStateOf(false) }
    var createSheet by remember { mutableStateOf(false) }
    var pickTopicForNote by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<TopicStats?>(null) }
    var deleteTarget by remember { mutableStateOf<TopicStats?>(null) }
    var sheetTarget by remember { mutableStateOf<TopicStats?>(null) }
    var markTarget by remember { mutableStateOf<TopicStats?>(null) }
    var openId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("随笔", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenAllNotes) {
                        Icon(SI.Article, contentDescription = "全部笔记")
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(SI.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(SI.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createSheet = true },
                icon = { Icon(SI.Add, contentDescription = null) },
                text = { Text("新建") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (topics.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                title = "还没有话题",
                hint = "建一个话题，比如「工作」「灵感」「读书笔记」\n笔记都放在话题里。"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    val open = openId == topic.id
                    SwipeRevealRow(
                        open = open,
                        onOpenChange = { openId = if (it) topic.id else null },
                        actionsWidth = ActionsWidth,
                        cornerRadius = 16.dp,
                        actions = {
                            SwipeAction(
                                icon = SI.PushPin,
                                label = if (topic.pinned) "取消置顶" else "置顶",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            ) {
                                openId = null
                                vm.togglePin(topic)
                            }
                            SwipeAction(
                                icon = SI.Brush,
                                label = "标记",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                            ) {
                                openId = null
                                markTarget = topic
                            }
                            SwipeAction(
                                icon = DeleteIcon,
                                label = "删除",
                                tint = MaterialTheme.colorScheme.error
                            ) {
                                openId = null
                                deleteTarget = topic
                            }
                        }
                    ) {
                        TopicCard(
                            topic = topic,
                            relativeTime = general.relativeTime,
                            onClick = {
                                if (open) openId = null else onOpenTopic(topic.id)
                            },
                            onLongClick = {
                                openId = null
                                sheetTarget = topic
                            }
                        )
                    }
                }
            }
        }
    }

    if (createSheet) {
        ActionSheet(
            onDismiss = { createSheet = false },
            actions = listOf(
                SheetAction("新建话题", SI.Forum) { showCreate = true },
                SheetAction("新建笔记", SI.MenuBook) {
                    if (topics.isEmpty()) showCreate = true else pickTopicForNote = true
                }
            )
        )
    }

    if (pickTopicForNote) {
        TopicPickerDialog(
            topics = topics,
            onDismiss = { pickTopicForNote = false },
            onPick = { id ->
                pickTopicForNote = false
                onNewNote(id)
            }
        )
    }

    if (showCreate) {
        TextInputDialog(
            title = "新建话题",
            label = "话题名称",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                vm.create(name) { id -> onOpenTopic(id) }
            }
        )
    }

    renameTarget?.let { target ->
        TextInputDialog(
            title = "重命名话题",
            label = "话题名称",
            initial = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                vm.rename(target.id, name)
            }
        )
    }

    markTarget?.let { target ->
        MarkPickerDialog(
            current = target.color,
            onDismiss = { markTarget = null },
            onPick = { vm.setColor(target.id, it) }
        )
    }

    sheetTarget?.let { target ->
        ActionSheet(
            onDismiss = { sheetTarget = null },
            actions = listOf(
                SheetAction(
                    if (target.pinned) "取消置顶" else "置顶",
                    SI.PushPin
                ) { vm.togglePin(target) },
                SheetAction("标记颜色", SI.Brush) { markTarget = target },
                SheetAction("重命名", RenameIcon) { renameTarget = target },
                SheetAction("删除话题", DeleteIcon, destructive = true) {
                    deleteTarget = target
                }
            )
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = "删除话题",
            message = "将同时删除「${target.name}」下的 ${target.noteCount} 篇笔记及其讨论，此操作无法撤销。",
            confirmTwice = general.confirmDelete,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                vm.delete(target.id)
            }
        )
    }
}

/** 新建笔记前先选一个话题。 */
@Composable
private fun TopicPickerDialog(
    topics: List<TopicStats>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("放到哪个话题") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(topics, key = { it.id }) { t ->
                    val shape = RoundedCornerShape(12.dp)
                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable { onPick(t.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                SI.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = t.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicCard(
    topic: TopicStats,
    relativeTime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = MaterialTheme.colorScheme.surface
    val cardColor = topic.color
        ?.let { tintSurface(base, Color(it), dark) }
        ?: base

    Surface(
        shape = RowShape,
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (topic.color != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(topic.color))
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        SI.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = topic.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (topic.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            SI.PushPin,
                            contentDescription = "已置顶",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildMeta(topic, relativeTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                SI.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun buildMeta(topic: TopicStats, relative: Boolean): String {
    if (topic.noteCount == 0) return "还没有笔记"
    val whenText = topic.lastNoteAt?.let { timeText(it, relative) } ?: ""
    return if (whenText.isEmpty()) "${topic.noteCount} 篇" else "${topic.noteCount} 篇 · $whenText"
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue