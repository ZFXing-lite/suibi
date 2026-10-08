package com.yq.suibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC, id ASC")
    fun observeByNote(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE discussionId = :discussionId ORDER BY createdAt ASC, id ASC")
    fun observeByDiscussion(discussionId: Long): Flow<List<Attachment>>

    /** 一篇笔记下所有讨论的附件，一次拿回来在内存里按 discussionId 分组。 */
    @Query(
        """
        SELECT a.* FROM attachments a
        INNER JOIN discussions d ON a.discussionId = d.id
        WHERE d.noteId = :noteId
        ORDER BY a.createdAt ASC, a.id ASC
        """
    )
    fun observeByNoteDiscussions(noteId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun getByNote(noteId: Long): List<Attachment>

    @Query("SELECT * FROM attachments WHERE discussionId = :discussionId")
    suspend fun getByDiscussion(discussionId: Long): List<Attachment>

    @Query("SELECT * FROM attachments ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<Attachment>

    @Query("SELECT COUNT(*) FROM attachments WHERE noteId = :noteId")
    suspend fun countByNote(noteId: Long): Int

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()
}