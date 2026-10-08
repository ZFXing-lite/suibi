package com.yq.suibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        """
        SELECT n.id AS id, n.topicId AS topicId, n.title AS title, n.content AS content,
               n.pinned AS pinned, n.color AS color,
               n.createdAt AS createdAt, n.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM discussions d WHERE d.noteId = n.id) AS discussionCount
        FROM notes n WHERE n.topicId = :topicId
        ORDER BY n.pinned DESC, n.updatedAt DESC
        """
    )
    fun observeByTopic(topicId: Long): Flow<List<NoteStats>>

    @Query(
        """
        SELECT n.id AS id, n.topicId AS topicId, n.title AS title, n.content AS content,
               n.pinned AS pinned, n.color AS color,
               n.createdAt AS createdAt, n.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM discussions d WHERE d.noteId = n.id) AS discussionCount
        FROM notes n WHERE n.topicId = :topicId
          AND (n.title LIKE '%' || :kw || '%' ESCAPE '\'
               OR n.content LIKE '%' || :kw || '%' ESCAPE '\')
        ORDER BY n.pinned DESC, n.updatedAt DESC
        """
    )
    fun searchInTopic(topicId: Long, kw: String): Flow<List<NoteStats>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT * FROM notes ORDER BY id ASC")
    suspend fun getAll(): List<Note>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE notes SET color = :color WHERE id = :id")
    suspend fun setColor(id: Long, color: Int?)

    @Query("UPDATE notes SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notes WHERE topicId = :topicId")
    suspend fun countInTopic(topicId: Long): Int

    /* ---------- 跨话题视图 ---------- */

    @Query(
        """
        SELECT n.id AS id, n.topicId AS topicId, t.name AS topicName,
               n.title AS title, n.content AS content,
               n.pinned AS pinned, n.color AS color,
               n.createdAt AS createdAt, n.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM discussions d WHERE d.noteId = n.id) AS discussionCount
        FROM notes n JOIN topics t ON t.id = n.topicId
        ORDER BY n.pinned DESC, n.updatedAt DESC
        """
    )
    fun observeAllWithTopic(): Flow<List<NoteWithTopic>>

    @Query(
        """
        SELECT n.id AS id, n.topicId AS topicId, t.name AS topicName,
               n.title AS title, n.content AS content,
               n.pinned AS pinned, n.color AS color,
               n.createdAt AS createdAt, n.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM discussions d WHERE d.noteId = n.id) AS discussionCount
        FROM notes n JOIN topics t ON t.id = n.topicId
        WHERE n.title LIKE '%' || :kw || '%' ESCAPE '\'
           OR n.content LIKE '%' || :kw || '%' ESCAPE '\'
           OR t.name LIKE '%' || :kw || '%' ESCAPE '\'
        ORDER BY n.pinned DESC, n.updatedAt DESC
        """
    )
    fun searchAllWithTopic(kw: String): Flow<List<NoteWithTopic>>
}