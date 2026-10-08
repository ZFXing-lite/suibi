package com.yq.suibi.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.Note
import com.yq.suibi.data.NoteStats
import com.yq.suibi.data.Topic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val db: AppDatabase,
    private val topicId: Long
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _topic = MutableStateFlow<Topic?>(null)
    val topic: StateFlow<Topic?> = _topic.asStateFlow()

    val notes: StateFlow<List<NoteStats>> = _query
        .flatMapLatest { q ->
            val kw = escapeLike(q.trim())
            if (kw.isEmpty()) db.noteDao().observeByTopic(topicId)
            else db.noteDao().searchInTopic(topicId, kw)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _topic.value = db.topicDao().get(topicId) }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun createNote(onCreated: (Long) -> Unit) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val id = db.noteDao().insert(
                Note(topicId = topicId, createdAt = now, updatedAt = now)
            )
            onCreated(id)
        }
    }

    fun togglePin(note: NoteStats) {
        viewModelScope.launch { db.noteDao().setPinned(note.id, !note.pinned) }
    }

    fun setColor(id: Long, color: Int?) {
        viewModelScope.launch { db.noteDao().setColor(id, color) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { db.noteDao().delete(id) }
    }
}

/** 转义 LIKE 通配符，避免用户输入的 % 和 _ 被当成模式。 */
internal fun escapeLike(raw: String): String =
    raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")