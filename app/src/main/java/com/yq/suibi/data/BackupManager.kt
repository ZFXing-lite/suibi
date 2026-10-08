package com.yq.suibi.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 全量备份：整库导出成一个 gzip 压缩的 JSON，传到 WebDAV。
 * 恢复：同格式文件覆盖本地全库。
 */
object BackupManager {

    private const val FORMAT = "suibi-backup"
    private const val VERSION = 1
    const val FILE_NAME = "suibi-backup.json.gz"

    data class Counts(val topics: Int, val notes: Int, val discussions: Int)

    suspend fun export(db: AppDatabase): ByteArray {
        val topics = db.topicDao().getAll()
        val notes = db.noteDao().getAll()
        val discussions = db.discussionDao().getAll()

        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
        }

        root.put("topics", JSONArray().apply {
            topics.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("sortOrder", t.sortOrder)
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
        })

        root.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("topicId", n.topicId)
                    put("title", n.title)
                    put("content", n.content)
                    put("createdAt", n.createdAt)
                    put("updatedAt", n.updatedAt)
                })
            }
        })

        root.put("discussions", JSONArray().apply {
            discussions.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("noteId", d.noteId)
                    put("content", d.content)
                    put("sortOrder", d.sortOrder)
                    put("createdAt", d.createdAt)
                    put("updatedAt", d.updatedAt)
                })
            }
        })

        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    suspend fun peek(db: AppDatabase): Counts = Counts(
        topics = db.topicDao().getAll().size,
        notes = db.noteDao().getAll().size,
        discussions = db.discussionDao().getAll().size
    )

    /** 覆盖式恢复。返回一句给人看的结论。 */
    suspend fun restore(db: AppDatabase, bytes: ByteArray): String {
        val text = try {
            if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                GZIPInputStream(bytes.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                String(bytes, Charsets.UTF_8)
            }
        } catch (t: Throwable) {
            return "解压失败：${t.message}"
        }

        val root = runCatching { JSONObject(text) }.getOrElse { return "文件不是合法 JSON" }
        if (root.optString("format") != FORMAT) return "文件格式不匹配，不是「随笔」的备份"

        var nt = 0
        var nn = 0
        var nd = 0

        db.withTransaction {
            db.discussionDao().deleteAll()
            db.noteDao().deleteAll()
            db.topicDao().deleteAll()

            root.optJSONArray("topics")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.topicDao().insert(
                        Topic(
                            id = o.optLong("id"),
                            name = o.optString("name"),
                            sortOrder = o.optInt("sortOrder"),
                            createdAt = o.optLong("createdAt"),
                            updatedAt = o.optLong("updatedAt")
                        )
                    )
                    nt++
                }
            }

            root.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.noteDao().insert(
                        Note(
                            id = o.optLong("id"),
                            topicId = o.optLong("topicId"),
                            title = o.optString("title"),
                            content = o.optString("content"),
                            createdAt = o.optLong("createdAt"),
                            updatedAt = o.optLong("updatedAt")
                        )
                    )
                    nn++
                }
            }

            root.optJSONArray("discussions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.discussionDao().insert(
                        Discussion(
                            id = o.optLong("id"),
                            noteId = o.optLong("noteId"),
                            content = o.optString("content"),
                            sortOrder = o.optInt("sortOrder"),
                            createdAt = o.optLong("createdAt"),
                            updatedAt = o.optLong("updatedAt")
                        )
                    )
                    nd++
                }
            }
        }

        return "恢复完成：$nt 个话题 / $nn 篇笔记 / $nd 条讨论"
    }
}