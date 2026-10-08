package com.yq.suibi.ui.search

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.NoteHit
import com.yq.suibi.data.TopicStats
import com.yq.suibi.ui.common.EmptyState
import com.yq.suibi.ui.common.relativeTime
import com.yq.suibi.ui.common.snippet
import com.yq.suibi.ui.common.vmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    db: AppDatabase,
    onBack: () -> Unit,
    onOpenNote: (topicId: Long, noteId: Long, query: String) -> Unit,
    onOpenTopic: (Long) -> Unit
) {
    val vm: SearchViewModel = viewModel(factory = vmFactory { SearchViewModel(db) })
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("搜索") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::onQueryChange,
                    placeholder = { Text("搜索话题、笔记、讨论") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清空")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .focusRequester(focus)
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                query.isBlank() -> EmptyState(
                    title = "全库搜索",
                    hint = "输入关键词，会同时搜话题名、笔记标题与正文、以及所有讨论。"
                )

                searching && results.isEmpty -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                results.isEmpty -> EmptyState(
                    title = "没有结果",
                    hint = "换个关键词试试。"
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (results.topics.isNotEmpty()) {
                        item(key = "h-topics") { SectionHeader("话题") }
                        items(results.topics, key = { "t-${it.id}" }) { t ->
                            TopicHitCard(t) { onOpenTopic(t.id) }
                        }
                    }
                    if (results.notes.isNotEmpty()) {
                        item(key = "h-notes") { SectionHeader("笔记与讨论") }
                        items(results.notes, key = { "n-${it.noteId}" }) { n ->
                            NoteHitCard(n) { onOpenNote(n.topicId, n.noteId, query) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun TopicHitCard(topic: TopicStats, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${topic.noteCount} 篇",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoteHitCard(hit: NoteHit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = hit.title.ifBlank { "无标题" },
                style = MaterialTheme.typography.titleMedium,
                color = if (hit.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val preview = snippet(hit.content)
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
            Text(
                text = "${hit.topicName} · ${relativeTime(hit.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}