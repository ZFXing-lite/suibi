package com.yq.suibi.ui.allnotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.NoteWithTopic
import com.yq.suibi.ui.common.ActionSheet
import com.yq.suibi.ui.common.ConfirmDialog
import com.yq.suibi.ui.common.DeleteConfirmDialog
import com.yq.suibi.ui.common.DeleteIcon
import com.yq.suibi.ui.common.EmptyState
import com.yq.suibi.ui.common.MarkPickerDialog
import com.yq.suibi.ui.common.SheetAction
import com.yq.suibi.ui.common.SwipeAction
import com.yq.suibi.ui.common.SwipeRevealRow
import com.yq.suibi.ui.common.relativeTime
import com.yq.suibi.ui.common.snippet
import com.yq.suibi.ui.common.timeText
import com.yq.suibi.ui.common.vmFactory
import com.yq.suibi.ui.theme.tintSurface

private val RowShape = RoundedCornerShape(16.dp)
private val ActionsWidth = 74.dp * 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllNotesScreen(
    db: AppDatabase,
    onBack: () -> Unit,
    onOpenNote: (topicId: Long, noteId: Long) -> Unit
) {
    val vm: AllNotesViewModel = viewModel(factory = vmFactory { AllNotesViewModel(db) })
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val app = context.applicationContext as com.yq.suibi.SuibiApp
    val general by app.settings.general.collectAsStateWithLifecycle(
        initialValue = com.yq.suibi.data.GeneralConfig()
    )

    var searchOpen by remember { mutableStateOf(false) }
    var sheetTarget by remember { mutableStateOf<NoteWithTopic?>(null) }
    var deleteTarget by remember { mutableStateOf<NoteWithTopic?>(null) }
    var markTarget by remember { mutableStateOf<NoteWithTopic?>(null) }
    var openId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("全部笔记") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) vm.onQueryChange("")
                        }) {
                            Icon(
                                if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "搜索"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                AnimatedVisibility(visible = searchOpen) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::onQueryChange,
                        placeholder = { Text("在所有笔记里搜") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        when {
            notes.isEmpty() && query.isBlank() -> EmptyState(
                modifier = Modifier.padding(padding),
                title = "还没有笔记",
                hint = "去话题里写第一篇，这里会按时间汇总所有笔记。"
            )

            notes.isEmpty() -> EmptyState(
                modifier = Modifier.padding(padding),
                title = "没有匹配的笔记",
                hint = "换个关键词试试。"
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val open = openId == note.id
                    SwipeRevealRow(
                        open = open,
                        onOpenChange = { openId = if (it) note.id else null },
                        actionsWidth = ActionsWidth,
                        cornerRadius = 16.dp,
                        actions = {
                            SwipeAction(
                                icon = Icons.Rounded.PushPin,
                                label = if (note.pinned) "取消置顶" else "置顶",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            ) {
                                openId = null
                                vm.togglePin(note)
                            }
                            SwipeAction(
                                icon = Icons.Rounded.Brush,
                                label = "标记",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                            ) {
                                openId = null
                                markTarget = note
                            }
                            SwipeAction(
                                icon = DeleteIcon,
                                label = "删除",
                                tint = MaterialTheme.colorScheme.error
                            ) {
                                openId = null
                                deleteTarget = note
                            }
                        }
                    ) {
                        AllNoteCard(
                            note = note,
                            relativeTime = general.relativeTime,
                            onClick = {
                                if (open) openId = null
                                else onOpenNote(note.topicId, note.id)
                            },
                            onLongClick = {
                                openId = null
                                sheetTarget = note
                            }
                        )
                    }
                }
            }
        }
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
                    Icons.Rounded.PushPin
                ) { vm.togglePin(target) },
                SheetAction("标记颜色", Icons.Rounded.Brush) { markTarget = target },
                SheetAction("删除笔记", DeleteIcon, destructive = true) {
                    deleteTarget = target
                }
            )
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = "删除笔记",
            message = "「${target.title.ifBlank { "无标题" }}」及其 ${target.discussionCount} 条讨论会被一起删除。",
            confirmTwice = general.confirmDelete,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                vm.deleteNote(target.id)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllNoteCard(
    note: NoteWithTopic,
    relativeTime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = MaterialTheme.colorScheme.surface
    val cardColor = note.color
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
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {

            // 话题名 —— 跨话题视图才需要
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.color != null) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(note.color))
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = note.topicName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = note.title.ifBlank { "无标题" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (note.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "已置顶",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            val preview = snippet(note.content)
            if (preview.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeText(note.updatedAt, relativeTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (note.discussionCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.height(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${note.discussionCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue