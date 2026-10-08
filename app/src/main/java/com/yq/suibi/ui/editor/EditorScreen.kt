package com.yq.suibi.ui.editor

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.Discussion
import com.yq.suibi.export.NoteImage
import com.yq.suibi.ui.common.ActionSheet
import com.yq.suibi.ui.common.ConfirmDialog
import com.yq.suibi.ui.common.DeleteIcon
import com.yq.suibi.ui.common.EditIcon
import com.yq.suibi.ui.common.HighlightTransform
import com.yq.suibi.ui.common.SheetAction
import com.yq.suibi.ui.common.TextInputDialog
import com.yq.suibi.ui.common.absoluteTime
import com.yq.suibi.ui.common.findMatches
import com.yq.suibi.ui.common.vmFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    db: AppDatabase,
    appScope: CoroutineScope,
    topicId: Long,
    noteId: Long,
    initialQuery: String = "",
    onBack: () -> Unit
) {
    val vm: EditorViewModel = viewModel(
        key = "editor-$topicId-$noteId",
        factory = vmFactory { EditorViewModel(db, appScope, topicId, noteId) }
    )

    val titleState by vm.title.collectAsStateWithLifecycle()
    val contentState by vm.content.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val createdAt by vm.createdAt.collectAsStateWithLifecycle()
    val updatedAt by vm.updatedAt.collectAsStateWithLifecycle()
    val discussions by vm.discussions.collectAsStateWithLifecycle()
    val topicName by vm.topicName.collectAsStateWithLifecycle()

    var titleValue by remember { mutableStateOf(TextFieldValue("")) }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var seeded by remember { mutableStateOf(false) }

    // 必须等 loaded 为 true 再灌值，否则会读到 init 协程还没写完的空串。
    LaunchedEffect(loaded) {
        if (loaded && !seeded) {
            titleValue = TextFieldValue(titleState, TextRange(titleState.length))
            contentValue = TextFieldValue(contentState, TextRange(contentState.length))
            seeded = true
        }
    }

    /* ---------- 笔记内查找 ---------- */

    var findOpen by remember { mutableStateOf(initialQuery.isNotBlank()) }
    var findQuery by remember { mutableStateOf(initialQuery) }
    var currentHit by remember { mutableIntStateOf(0) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val matches = remember(contentValue.text, findQuery) {
        findMatches(contentValue.text, findQuery)
    }

    // 命中集合变了就把游标收回范围内。
    LaunchedEffect(matches.size) {
        if (matches.isEmpty()) currentHit = 0
        else if (currentHit !in matches.indices) currentHit = 0
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun jumpTo(index: Int) {
        val r = matches.getOrNull(index) ?: return
        contentValue = contentValue.copy(selection = TextRange(r.first, r.last + 1))
        val lr = layoutResult ?: return
        if (r.first > contentValue.text.length) return
        val lineTop = lr.getLineTop(lr.getLineForOffset(r.first))
        scope.launch {
            listState.scrollToItem(0, (lineTop - 180f).coerceAtLeast(0f).toInt())
        }
    }

    fun step(delta: Int) {
        if (matches.isEmpty()) return
        currentHit = ((currentHit + delta) % matches.size + matches.size) % matches.size
        jumpTo(currentHit)
    }

    /* ---------- 导出图片 ---------- */

    val context = LocalContext.current
    // 在组合作用域里先取好配色，导出的协程里不能再读 MaterialTheme。
    val scheme = MaterialTheme.colorScheme
    var exportOpen by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun exportTo(share: Boolean) {
        exportOpen = false
        if (exporting) return
        exporting = true
        scope.launch {
            val spec = NoteImage.Spec(
                title = titleValue.text,
                topicName = topicName,
                content = contentValue.text,
                discussions = discussions.map { it.content to it.createdAt },
                createdAt = createdAt,
                updatedAt = updatedAt,
                bg = scheme.background.toArgb(),
                onBg = scheme.onBackground.toArgb(),
                muted = scheme.outline.toArgb(),
                accent = scheme.primary.toArgb(),
                rule = scheme.outlineVariant.toArgb()
            )
            val bitmap = withContext(Dispatchers.Default) { NoteImage.render(spec) }
            val name = NoteImage.safeName(titleValue.text)

            if (share) {
                val uri = NoteImage.shareUri(context, bitmap, name)
                if (uri != null) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(send, "分享笔记").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    toast(context, "生成失败")
                }
            } else {
                val ok = NoteImage.saveToGallery(context, bitmap, name)
                toast(context, if (ok) "已存到相册 Pictures/随笔" else "保存失败，检查存储权限")
            }
            bitmap.recycle()
            exporting = false
        }
    }

    var discussOpen by remember { mutableStateOf(false) }
    var addDiscussion by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Discussion?>(null) }
    var deleteTarget by remember { mutableStateOf<Discussion?>(null) }
    var sheetTarget by remember { mutableStateOf<Discussion?>(null) }

    BackHandler {
        vm.flush()
        onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.flush()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = if (exporting) "生成中…" else "自动保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = {
                        findOpen = !findOpen
                        if (!findOpen) {
                            findQuery = ""
                            currentHit = 0
                        }
                    }) {
                        Icon(
                            if (findOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "笔记内查找",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { exportOpen = true }) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = "导出为图片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            TextField(
                value = titleValue,
                onValueChange = {
                    titleValue = it
                    vm.onTitleChange(it.text)
                },
                placeholder = {
                    Text(
                        "标题",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 时间署名：标题下一行
            if (createdAt > 0L) {
                Text(
                    text = if (updatedAt > createdAt) {
                        "创建于 ${absoluteTime(createdAt)} · 更新于 ${absoluteTime(updatedAt)}"
                    } else {
                        "创建于 ${absoluteTime(createdAt)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (findOpen) {
                FindBar(
                    query = findQuery,
                    onQueryChange = {
                        findQuery = it
                        currentHit = 0
                    },
                    index = currentHit,
                    total = matches.size,
                    onPrev = { step(-1) },
                    onNext = { step(1) },
                    onClose = {
                        findOpen = false
                        findQuery = ""
                        currentHit = 0
                    }
                )
            }

            MarkdownToolbar(
                onBold = { contentValue = Md.wrap(contentValue, "**").also { vm.onContentChange(it.text) } },
                onItalic = { contentValue = Md.wrap(contentValue, "*").also { vm.onContentChange(it.text) } },
                onHeading = { contentValue = Md.prefixLine(contentValue, "## ").also { vm.onContentChange(it.text) } },
                onBullet = { contentValue = Md.prefixLine(contentValue, "- ").also { vm.onContentChange(it.text) } },
                onOrdered = { contentValue = Md.prefixLine(contentValue, "1. ").also { vm.onContentChange(it.text) } },
                onQuote = { contentValue = Md.prefixLine(contentValue, "> ").also { vm.onContentChange(it.text) } },
                onCode = { contentValue = Md.wrap(contentValue, "`").also { vm.onContentChange(it.text) } },
                onDivider = { contentValue = Md.insertBlock(contentValue, "---").also { vm.onContentChange(it.text) } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "content") {
                    // 用 BasicTextField：Material3 的 TextField 不暴露 onTextLayout，
                    // 而滚动到命中处需要 layoutResult 算行位置。
                    BasicTextField(
                        value = contentValue,
                        onValueChange = {
                            contentValue = it
                            vm.onContentChange(it.text)
                        },
                        onTextLayout = { layoutResult = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = HighlightTransform(
                            ranges = matches,
                            currentIndex = currentHit,
                            normal = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            active = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        ),
                        decorationBox = { inner ->
                            Box {
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        text = "写点什么……",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                item(key = "discussion-header") {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    DiscussionHeader(
                        count = discussions.size,
                        expanded = discussOpen,
                        onToggle = { discussOpen = !discussOpen }
                    )
                }

                if (discussOpen) {
                    items(discussions, key = { "d-${it.id}" }) { d ->
                        DiscussionCard(
                            discussion = d,
                            onLongClick = { sheetTarget = d }
                        )
                    }

                    item(key = "add-discussion") {
                        TextButton(
                            onClick = { addDiscussion = true },
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("添加讨论", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (exportOpen) {
        ActionSheet(
            onDismiss = { exportOpen = false },
            actions = listOf(
                SheetAction("保存到相册", Icons.Rounded.Image) { exportTo(share = false) },
                SheetAction("分享图片", Icons.Rounded.Image) { exportTo(share = true) }
            )
        )
    }

    if (addDiscussion) {
        TextInputDialog(
            title = "添加讨论",
            label = "讨论内容",
            multiline = true,
            onDismiss = { addDiscussion = false },
            onConfirm = { text ->
                addDiscussion = false
                vm.addDiscussion(text)
                discussOpen = true
            }
        )
    }

    editTarget?.let { target ->
        TextInputDialog(
            title = "编辑讨论",
            label = "讨论内容",
            initial = target.content,
            multiline = true,
            onDismiss = { editTarget = null },
            onConfirm = { text ->
                editTarget = null
                vm.updateDiscussion(target.id, text)
            }
        )
    }

    sheetTarget?.let { target ->
        ActionSheet(
            onDismiss = { sheetTarget = null },
            actions = listOf(
                SheetAction("编辑", EditIcon) { editTarget = target },
                SheetAction("删除", DeleteIcon, destructive = true) { deleteTarget = target }
            )
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除讨论",
            message = "这条讨论会被永久删除。",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                vm.deleteDiscussion(target.id)
            }
        )
    }
}

/* ---------- 查找栏 ---------- */

@Composable
private fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    index: Int,
    total: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text("在这篇里找", style = MaterialTheme.typography.bodyMedium)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = if (total == 0) "无" else "${index + 1}/$total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = onPrev,
                enabled = total > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "上一处",
                    tint = if (total > 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }

            IconButton(
                onClick = onNext,
                enabled = total > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "下一处",
                    tint = if (total > 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.outlineVariant
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭查找",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------- 讨论区 ---------- */

@Composable
private fun DiscussionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickableSafe(onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            Icons.Rounded.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (count == 0) "讨论" else "讨论 ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscussionCard(
    discussion: Discussion,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = discussion.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = absoluteTime(discussion.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/* ---------- 小工具 ---------- */

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255f + 0.5f).toInt(),
    (red * 255f + 0.5f).toInt(),
    (green * 255f + 0.5f).toInt(),
    (blue * 255f + 0.5f).toInt()
)

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}