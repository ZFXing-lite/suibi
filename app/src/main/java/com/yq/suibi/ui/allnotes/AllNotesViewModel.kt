package com.yq.suibi.ui.allnotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.NoteWithTopic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AllNotesViewModel(private val db: AppDatabase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val notes: StateFlow<List<NoteWithTopic>> = _query
        .flatMapLatest { q ->
            val kw = escapeLikeAll(q.trim())
            if (kw.isEmpty()) db.noteDao().observeAllWithTopic()
            else db.noteDao().searchAllWithTopic(kw)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun togglePin(note: NoteWithTopic) {
        viewModelScope.launch { db.noteDao().setPinned(note.id, !note.pinned) }
    }

    fun setColor(id: Long, color: Int?) {
        viewModelScope.launch { db.noteDao().setColor(id, color) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { db.noteDao().delete(id) }
    }
}

/** 与 NoteListViewModel 里的转义规则保持一致。 */
internal fun escapeLikeAll(raw: String): String =
    raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")