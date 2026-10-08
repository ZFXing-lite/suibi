package com.yq.suibi.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.Discussion
import com.yq.suibi.data.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EditorViewModel(
    private val db: AppDatabase,
    private val appScope: CoroutineScope,
    private val topicId: Long,
    noteId: Long
) : ViewModel() {

    private val _noteId = MutableStateFlow(noteId)
    val noteId: StateFlow<Long> = _noteId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    /**
     * 是否已经从库里读完。
     *
     * 界面必须等这个变成 true 才能把内容灌进输入框 —— 否则会在
     * init 的协程还没跑完时读到空串，把已有笔记显示成空白页。
     */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _createdAt = MutableStateFlow(0L)
    val createdAt: StateFlow<Long> = _createdAt.asStateFlow()

    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt.asStateFlow()

    /** 导出长图时要印上话题名。 */
    private val _topicName = MutableStateFlow("")
    val topicName: StateFlow<String> = _topicName.asStateFlow()

    val discussions: StateFlow<List<Discussion>> = _noteId
        .flatMapLatest { id ->
            if (id <= 0L) flowOf(emptyList()) else db.discussionDao().observeByNote(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val saveSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch {
            val id = _noteId.value
            if (id > 0L) {
                db.noteDao().get(id)?.let { n ->
                    _title.value = n.title
                    _content.value = n.content
                    _createdAt.value = n.createdAt
                    _updatedAt.value = n.updatedAt
                }
            }
            // 新笔记也要放行，否则输入框永远不激活。
            _loaded.value = true
        }

        viewModelScope.launch {
            _topicName.value = db.topicDao().get(topicId)?.name ?: ""
        }

        viewModelScope.launch {
            saveSignal.debounce(700L).collect { persist() }
        }
    }

    fun onTitleChange(value: String) {
        _title.value = value
        _updatedAt.value = System.currentTimeMillis()
        saveSignal.tryEmit(Unit)
    }

    fun onContentChange(value: String) {
        _content.value = value
        _updatedAt.value = System.currentTimeMillis()
        saveSignal.tryEmit(Unit)
    }

    /** 立即落库，用于返回键或页面销毁。 */
    fun flush() {
        appScope.launch { persist() }
    }

    private suspend fun persist() {
        val t = _title.value
        val c = _content.value
        val now = System.currentTimeMillis()
        val existing = _noteId.value

        if (existing > 0L) {
            db.noteDao().get(existing)?.let { n ->
                if (n.title != t || n.content != c) {
                    db.noteDao().update(n.copy(title = t, content = c, updatedAt = now))
                    _updatedAt.value = now
                }
            }
        } else {
            if (t.isBlank() && c.isBlank()) return
            val id = db.noteDao().insert(
                Note(topicId = topicId, title = t, content = c, createdAt = now, updatedAt = now)
            )
            _noteId.value = id
            _createdAt.value = now
            _updatedAt.value = now
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 用户已经离开页面，用应用级作用域把最后一次编辑补上。
        appScope.launch { persist() }
    }

    /* ---------- 讨论 ---------- */

    private suspend fun requireNoteId(): Long? {
        persist()
        val id = _noteId.value
        return if (id > 0L) id else null
    }

    fun addDiscussion(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            val id = requireNoteId() ?: return@launch
            val now = System.currentTimeMillis()
            val order = db.discussionDao().count(id)
            db.discussionDao().insert(
                Discussion(noteId = id, content = body, sortOrder = order, createdAt = now, updatedAt = now)
            )
            touchNote(now)
        }
    }

    fun updateDiscussion(id: Long, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            db.discussionDao().updateContent(id, body, System.currentTimeMillis())
        }
    }

    fun deleteDiscussion(id: Long) {
        viewModelScope.launch {
            db.discussionDao().delete(id)
            touchNote(System.currentTimeMillis())
        }
    }

    private suspend fun touchNote(now: Long) {
        val id = _noteId.value
        if (id <= 0L) return
        db.noteDao().get(id)?.let { db.noteDao().update(it.copy(updatedAt = now)) }
        _updatedAt.value = now
    }
}