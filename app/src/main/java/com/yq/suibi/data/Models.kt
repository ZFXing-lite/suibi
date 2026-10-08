package com.yq.suibi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    val color: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val topicId: Long,
    val title: String = "",
    val content: String = "",
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    val color: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "discussions",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Discussion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val content: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/* ---------- query projections ---------- */

data class TopicStats(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val pinned: Boolean,
    val color: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val noteCount: Int,
    val lastNoteAt: Long?
)

data class NoteStats(
    val id: Long,
    val topicId: Long,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val color: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val discussionCount: Int
)

data class NoteHit(
    val noteId: Long,
    val topicId: Long,
    val topicName: String,
    val title: String,
    val content: String,
    val updatedAt: Long
)

/** 跨话题的笔记行，用于「全部笔记」视图。 */
data class NoteWithTopic(
    val id: Long,
    val topicId: Long,
    val topicName: String,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val color: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val discussionCount: Int
)

data class DiscussionHit(
    val noteId: Long,
    val topicId: Long,
    val topicName: String,
    val title: String,
    val snippet: String,
    val updatedAt: Long
)