package com.yq.suibi.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 附件的磁盘侧。
 *
 * 选中的图片/文件会被复制进 filesDir/attachments/，用随机名，
 * 数据库只存相对路径 —— 沙箱路径变了也不影响，而且不依赖
 * 用户后来删掉的那个原始文件。
 */
object AttachmentStore {

    private const val DIR = "attachments"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun resolve(context: Context, relPath: String): File = File(context.filesDir, relPath)

    /** 从 content:// 或 file:// 复制进来，返回可入库的附件描述。 */
    suspend fun import(
        context: Context,
        uri: Uri,
        owner: Owner
    ): Attachment? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val name = queryName(context, uri) ?: "未命名"
            val mime = resolver.getType(uri) ?: guessMime(name)
            val ext = extensionFor(name, mime)
            val fileName = "${UUID.randomUUID()}$ext"

            val target = File(dir(context), fileName)
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            val (noteId, discussionId) = when (owner) {
                is Owner.OfNote -> owner.noteId to null
                is Owner.OfDiscussion -> null to owner.discussionId
            }

            Attachment(
                noteId = noteId,
                discussionId = discussionId,
                kind = if (mime.startsWith("image/")) AttachmentKind.IMAGE else AttachmentKind.FILE,
                displayName = name,
                mimeType = mime,
                size = target.length(),
                relPath = "$DIR/$fileName",
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFile(context: Context, relPath: String) {
        try {
            resolve(context, relPath).delete()
        } catch (_: Exception) {
        }
    }

    /**
     * 清掉没有数据库行指向的孤儿文件。
     * 删笔记/删讨论走的是 CASCADE，数据库行没了但文件还在，
     * 这里兜底回收。应用启动时跑一次。
     */
    suspend fun sweepOrphans(context: Context, referenced: Set<String>) = withContext(Dispatchers.IO) {
        try {
            dir(context).listFiles()?.forEach { f ->
                val rel = "$DIR/${f.name}"
                if (rel !in referenced) f.delete()
            }
        } catch (_: Exception) {
        }
    }

    /* ---------- 辅助 ---------- */

    sealed interface Owner {
        data class OfNote(val noteId: Long) : Owner
        data class OfDiscussion(val discussionId: Long) : Owner
    }

    fun queryName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        } ?: uri.lastPathSegment
    } catch (e: Exception) {
        uri.lastPathSegment
    }

    fun querySize(context: Context, uri: Uri): Long = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && c.moveToFirst() && !c.isNull(idx)) c.getLong(idx) else 0L
        } ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun extensionFor(name: String, mime: String): String {
        val fromName = name.substringAfterLast('.', "")
        if (fromName.isNotEmpty() && fromName.length <= 8) return ".$fromName"
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ".$it" } ?: ""
    }

    private fun guessMime(name: String): String =
        MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

    /** 人类可读大小：1.2 MB / 340 KB / 512 B */
    fun humanSize(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
        bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
        bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}