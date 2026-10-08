package com.yq.suibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {

    @Query("SELECT * FROM discussions WHERE noteId = :noteId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByNote(noteId: Long): Flow<List<Discussion>>

    @Query("SELECT * FROM discussions WHERE noteId = :noteId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getByNote(noteId: Long): List<Discussion>

    @Query("SELECT * FROM discussions ORDER BY id ASC")
    suspend fun getAll(): List<Discussion>

    @Query("SELECT COUNT(*) FROM discussions WHERE noteId = :noteId")
    suspend fun count(noteId: Long): Int

    @Insert
    suspend fun insert(discussion: Discussion): Long

    @Update
    suspend fun update(discussion: Discussion)

    @Query("UPDATE discussions SET content = :content, updatedAt = :now WHERE id = :id")
    suspend fun updateContent(id: Long, content: String, now: Long)

    @Query("DELETE FROM discussions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM discussions")
    suspend fun deleteAll()
}