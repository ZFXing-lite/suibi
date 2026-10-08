package com.yq.suibi.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SearchDao {

    @Query(
        """
        SELECT n.id AS noteId, n.topicId AS topicId, t.name AS topicName,
               n.title AS title, n.content AS content, n.updatedAt AS updatedAt
        FROM notes n JOIN topics t ON t.id = n.topicId
        WHERE n.title LIKE '%' || :kw || '%' ESCAPE '\'
           OR n.content LIKE '%' || :kw || '%' ESCAPE '\'
        ORDER BY n.updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchNotes(kw: String, limit: Int): List<NoteHit>

    @Query(
        """
        SELECT n.id AS noteId, n.topicId AS topicId, t.name AS topicName,
               n.title AS title, d.content AS snippet, n.updatedAt AS updatedAt
        FROM discussions d
             JOIN notes n ON n.id = d.noteId
             JOIN topics t ON t.id = n.topicId
        WHERE d.content LIKE '%' || :kw || '%' ESCAPE '\'
        ORDER BY n.updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun searchDiscussions(kw: String, limit: Int): List<DiscussionHit>

    @Query(
        """
        SELECT t.id AS id, t.name AS name, t.sortOrder AS sortOrder,
               t.pinned AS pinned, t.color AS color,
               t.createdAt AS createdAt, t.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM notes n WHERE n.topicId = t.id) AS noteCount,
               (SELECT MAX(n.updatedAt) FROM notes n WHERE n.topicId = t.id) AS lastNoteAt
        FROM topics t
        WHERE t.name LIKE '%' || :kw || '%' ESCAPE '\'
        ORDER BY t.pinned DESC, t.sortOrder ASC
        LIMIT :limit
        """
    )
    suspend fun searchTopics(kw: String, limit: Int): List<TopicStats>
}