package com.yq.suibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query(
        """
        SELECT t.id AS id, t.name AS name, t.sortOrder AS sortOrder,
               t.pinned AS pinned, t.color AS color,
               t.createdAt AS createdAt, t.updatedAt AS updatedAt,
               (SELECT COUNT(*) FROM notes n WHERE n.topicId = t.id) AS noteCount,
               (SELECT MAX(n.updatedAt) FROM notes n WHERE n.topicId = t.id) AS lastNoteAt
        FROM topics t
        ORDER BY t.pinned DESC, t.sortOrder ASC, t.createdAt ASC
        """
    )
    fun observeAll(): Flow<List<TopicStats>>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun get(id: Long): Topic?

    @Query("SELECT * FROM topics ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAll(): List<Topic>

    @Insert
    suspend fun insert(topic: Topic): Long

    @Update
    suspend fun update(topic: Topic)

    @Query("UPDATE topics SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("UPDATE topics SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE topics SET color = :color WHERE id = :id")
    suspend fun setColor(id: Long, color: Int?)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM topics")
    suspend fun deleteAll()
}