package com.yq.suibi.ui.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.Topic
import com.yq.suibi.data.TopicStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicListViewModel(private val db: AppDatabase) : ViewModel() {

    val topics: StateFlow<List<TopicStats>> = db.topicDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = db.topicDao().insert(
                Topic(name = trimmed, createdAt = now, updatedAt = now)
            )
            onCreated(id)
        }
    }

    fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            db.topicDao().rename(id, trimmed, System.currentTimeMillis())
        }
    }

    fun togglePin(topic: TopicStats) {
        viewModelScope.launch { db.topicDao().setPinned(topic.id, !topic.pinned) }
    }

    fun setColor(id: Long, color: Int?) {
        viewModelScope.launch { db.topicDao().setColor(id, color) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { db.topicDao().delete(id) }
    }

    suspend fun noteCount(id: Long): Int = db.noteDao().countInTopic(id)
}