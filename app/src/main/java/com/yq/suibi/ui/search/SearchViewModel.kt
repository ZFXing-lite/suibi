package com.yq.suibi.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.NoteHit
import com.yq.suibi.data.TopicStats
import com.yq.suibi.ui.notes.escapeLike
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SearchResults(
    val topics: List<TopicStats> = emptyList(),
    val notes: List<NoteHit> = emptyList(),
    val discussions: List<NoteHit> = emptyList()
) {
    val isEmpty: Boolean get() = topics.isEmpty() && notes.isEmpty() && discussions.isEmpty()
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val db: AppDatabase) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(220L)
                .distinctUntilChanged()
                .collect { run(it) }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        if (value.isBlank()) _results.value = SearchResults()
    }

    private suspend fun run(raw: String) {
        val kw = escapeLike(raw.trim())
        if (kw.isEmpty()) {
            _results.value = SearchResults()
            _searching.value = false
            return
        }
        _searching.value = true
        val topics = db.searchDao().searchTopics(kw, 30)
        val notes = db.searchDao().searchNotes(kw, 200)
        val discs = db.searchDao().searchDiscussions(kw, 200)

        // 讨论命中归并入所属笔记，避免同一篇笔记重复出现
        val seen = notes.map { it.noteId }.toHashSet()
        val merged = ArrayList<NoteHit>(notes)
        for (d in discs) {
            if (seen.add(d.noteId)) {
                merged += NoteHit(
                    noteId = d.noteId,
                    topicId = d.topicId,
                    topicName = d.topicName,
                    title = d.title,
                    content = d.snippet,
                    updatedAt = d.updatedAt
                )
            }
        }
        merged.sortByDescending { it.updatedAt }

        _results.value = SearchResults(topics = topics, notes = merged, discussions = emptyList())
        _searching.value = false
    }
}