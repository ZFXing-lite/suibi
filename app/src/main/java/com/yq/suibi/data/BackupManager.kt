package com.yq.suibi.data

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 全量备份：整库 + 附件文件导出成一个 gzip 压缩的 JSON，传到 WebDAV。
 * 恢复：同格式文件覆盖本地全库。
 *
 * 附件以 base64 内嵌，所以备份文件是自包含的 —— 换手机恢复不会丢图。
 * 代价是体积，图片多的库备份会大一些，这是「全量」该付的钱。
 */
object BackupManager {

    private const val FORMAT = "suibi-backup"
    /** v2：补上 pinned/color，并内嵌附件文件。v1 的备份仍可恢复。 */
    private const val VERSION = 2
    const val FILE_NAME = "suibi-backup.json.gz"

    data class Counts(
        val topics: Int,
        val notes: Int,
        val discussions: Int,
        val attachments: Int = 0
    )

    suspend fun export(context: Context, db: AppDatabase): ByteArray {
        val topics = db.topicDao().getAll()
        val notes = db.noteDao().getAll()
        val discussions = db.discussionDao().getAll()
        val attachments = db.attachmentDao().getAll()

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
                    put("pinned", t.pinned)
                    put("color", t.color ?: JSONObject.NULL)
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
                    put("pinned", n.pinned)
                    put("color", n.color ?: JSONObject.NULL)
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

        root.put("attachments", JSONArray().apply {
            attachments.forEach { a ->
                // 文件读不到就跳过这条，不让整个备份失败
                val file = AttachmentStore.resolve(context, a.relPath)
                if (!file.exists() || !file.canRead()) return@forEach
                val data = runCatching { file.readBytes() }.getOrNull() ?: return@forEach
                put(JSONObject().apply {
                    put("id", a.id)
                    put("noteId", a.noteId ?: JSONObject.NULL)
                    put("discussionId", a.discussionId ?: JSONObject.NULL)
                    put("kind", a.kind)
                    put("displayName", a.displayName)
                    put("mimeType", a.mimeType)
                    put("size", a.size)
                    put("relPath", a.relPath)
                    put("createdAt", a.createdAt)
                    put("data", Base64.encodeToString(data, Base64.NO_WRAP))
                })
            }
        })

        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    suspend fun peek(context: Context, db: AppDatabase): Counts = Counts(
        topics = db.topicDao().getAll().size,
        notes = db.noteDao().getAll().size,
        discussions = db.discussionDao().getAll().size,
        attachments = db.attachmentDao().getAll().size
    )

    /** 覆盖式恢复。返回一句给人看的结论。 */
    suspend fun restore(context: Context, db: AppDatabase, bytes: ByteArray): String {
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
        var na = 0

        // 附件先解出来写盘，再入库 —— 这样不会出现「库里有行、盘上没文件」。
        val pending = mutableListOf<Pair<Attachment, ByteArray>>()
        root.optJSONArray("attachments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val b64 = o.optString("data")
                if (b64.isEmpty()) continue
                val data = runCatching { Base64.decode(b64, Base64.NO_WRAP) }.getOrNull() ?: continue
                val rel = o.optString("relPath")
                if (rel.isEmpty()) continue
                pending.add(
                    Attachment(
                        id = o.optLong("id"),
                        noteId = if (o.isNull("noteId")) null else o.optLong("noteId"),
                        discussionId = if (o.isNull("discussionId")) null else o.optLong("discussionId"),
                        kind = o.optString("kind", AttachmentKind.FILE),
                        displayName = o.optString("displayName"),
                        mimeType = o.optString("mimeType", "application/octet-stream"),
                        size = o.optLong("size"),
                        relPath = rel,
                        createdAt = o.optLong("createdAt")
                    ) to data
                )
            }
        }

        db.withTransaction {
            db.discussionDao().deleteAll()
            db.noteDao().deleteAll()
            db.topicDao().deleteAll()
            db.attachmentDao().deleteAll()

            root.optJSONArray("topics")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.topicDao().insert(
                        Topic(
                            id = o.optLong("id"),
                            name = o.optString("name"),
                            sortOrder = o.optInt("sortOrder"),
                            pinned = o.optBoolean("pinned", false),
                            color = if (o.isNull("color")) null else o.optInt("color"),
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
                            pinned = o.optBoolean("pinned", false),
                            color = if (o.isNull("color")) null else o.optInt("color"),
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

            db.attachmentDao().deleteAll()
            pending.forEach { (a, _) ->
                db.attachmentDao().insert(a)
                na++
            }
        }

        // 事务成功之后才落盘，避免回滚后留下一堆孤儿文件。
        pending.forEach { (a, data) ->
            runCatching {
                val f = AttachmentStore.resolve(context, a.relPath)
                f.parentFile?.mkdirs()
                f.writeBytes(data)
            }
        }

        return "恢复完成：$nt 个话题 / $nn 篇笔记 / $nd 条讨论 / $na 个附件"
    }
}