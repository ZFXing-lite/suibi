package com.yq.suibi.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 把一篇笔记渲染成一张长图。
 *
 * 排版交给 StaticLayout —— 中文换行、行高、字距它算得准，
 * 自己量宽度迟早出偏差。先建块、量总高，再开 Bitmap 一次画完。
 *
 * 正文做一次轻量 Markdown 渲染：标题、列表、引用、粗体、斜体、
 * 行内代码、分隔线。不做完整规范，只做日常真的会用的那几种。
 */
object NoteImage {

    private const val WIDTH = 1080
    private const val PAD = 72
    private const val MAX_HEIGHT = 12000
    private const val TEXT_WIDTH = WIDTH - PAD * 2

    private val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    /** 一篇文章 + 一套配色 = 一次渲染。 */
    data class Spec(
        val title: String,
        val topicName: String,
        val content: String,
        val discussions: List<Pair<String, Long>>,
        val createdAt: Long,
        val updatedAt: Long,
        val bg: Int,
        val onBg: Int,
        val muted: Int,
        val accent: Int,
        val rule: Int,
        /** 输出宽度（px）。排版按 1080 逻辑坐标写，靠画布缩放适配。 */
        val width: Int = WIDTH
    )

    /* ---------- 渲染 ---------- */

    fun render(spec: Spec): Bitmap {
        val b = Builder(spec)
        return b.build()
    }

    private class Builder(private val spec: Spec) {

        private val blocks = mutableListOf<Block>()

        fun build(): Bitmap {
            text(spec.topicName, 32f, spec.accent, bold = true, gapAfter = 14f)
            text(
                spec.title.ifBlank { "无标题" },
                64f,
                if (spec.title.isBlank()) spec.muted else spec.onBg,
                bold = true, gapAfter = 18f, lineMult = 1.2f
            )

            val meta = if (spec.updatedAt > spec.createdAt) {
                "创建于 ${fmt.format(Date(spec.createdAt))} · 更新于 ${fmt.format(Date(spec.updatedAt))}"
            } else {
                "创建于 ${fmt.format(Date(spec.createdAt))}"
            }
            text(meta, 28f, spec.muted, gapAfter = 26f)

            rule(gapBefore = 0f, gapAfter = 30f)

            if (spec.content.isBlank()) {
                text("（空）", 42f, spec.muted, italic = true, gapAfter = 20f)
            } else {
                spec.content.split("\n").forEach { line -> line(line) }
            }

            if (spec.discussions.isNotEmpty()) {
                rule(gapBefore = 24f, gapAfter = 30f)
                text("讨论 (${spec.discussions.size})", 34f, spec.accent, bold = true, gapAfter = 20f)
                spec.discussions.forEach { (body, ts) ->
                    text(body, 38f, spec.onBg, gapBefore = 8f, gapAfter = 8f)
                    text(fmt.format(Date(ts)), 26f, spec.muted, gapAfter = 22f)
                }
            }

            rule(gapBefore = 28f, gapAfter = 30f)
            text("随笔", 28f, spec.muted, gapAfter = 0f)

            var height = (PAD * 2).toFloat()
            blocks.forEach { height += it.totalHeight() }

            // 一切按 1080 的逻辑坐标排版，最后整块画布缩放。
            // 这样字号是矢量缩放，放大到 1440 也不会糊。
            val outWidth = spec.width.coerceIn(720, 2160)
            val scale = outWidth.toFloat() / WIDTH
            val maxHeight = (MAX_HEIGHT * scale).toInt()
            val finalHeight = (height * scale).toInt().coerceAtMost(maxHeight)

            val bmp = Bitmap.createBitmap(outWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(spec.bg)
            canvas.scale(scale, scale)

            var y = PAD.toFloat()
            for (blk in blocks) {
                y += blk.gapBefore
                when (blk) {
                    is Block.Text -> {
                        canvas.save()
                        canvas.translate(PAD + blk.indent, y)
                        blk.layout.draw(canvas)
                        canvas.restore()
                        y += blk.layout.height
                    }
                    is Block.Rule -> {
                        canvas.drawLine(
                            PAD.toFloat(), y,
                            (WIDTH - PAD).toFloat(), y,
                            Paint().apply {
                                color = spec.rule
                                strokeWidth = blk.thickness.toFloat()
                            }
                        )
                        y += blk.thickness
                    }
                    is Block.Gap -> {
                        y += blk.height
                    }
                }
                y += blk.gapAfter
                if (y * scale > finalHeight) break
            }
            return bmp
        }

        private fun text(
            src: String,
            size: Float,
            color: Int,
            bold: Boolean = false,
            italic: Boolean = false,
            indent: Float = 0f,
            gapBefore: Float = 0f,
            gapAfter: Float = 0f,
            lineMult: Float = 1.35f
        ) {
            if (src.isBlank()) return
            val paint = TextPaint().apply {
                isAntiAlias = true
                textSize = size
                this.color = color
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    if (bold) Typeface.BOLD else Typeface.NORMAL
                )
            }
            if (italic) paint.textSkewX = -0.25f

            val (plain, spans) = parseInline(src)
            val sp = SpannableString(plain)
            spans.forEach { (s, e, span) -> sp.setSpan(span, s, e, 0) }

            val layout = StaticLayout.Builder
                .obtain(sp, 0, sp.length, paint, (TEXT_WIDTH - indent).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineMult)
                .setIncludePad(false)
                .build()
            blocks.add(Block.Text(layout, indent, gapBefore, gapAfter))
        }

        private fun rule(gapBefore: Float, gapAfter: Float) {
            blocks.add(Block.Rule(1, gapBefore, gapAfter))
        }

        /** 空行也要占高度，否则段落会挤在一起。 */
        private fun spacer(height: Float) {
            blocks.add(Block.Gap(height))
        }

        /** 行级标记：标题 / 列表 / 引用 / 分隔线，其余按正文。 */
        private fun line(raw: String) {
            val s = raw.trimEnd()
            when {
                s.isBlank() -> spacer(22f)

                s == "---" || s == "***" || s == "___" -> rule(14f, 20f)

                s.startsWith("### ") ->
                    text(s.removePrefix("### "), 42f, spec.onBg, bold = true, gapBefore = 12f, gapAfter = 10f)

                s.startsWith("## ") ->
                    text(s.removePrefix("## "), 48f, spec.onBg, bold = true, gapBefore = 16f, gapAfter = 12f)

                s.startsWith("# ") ->
                    text(s.removePrefix("# "), 54f, spec.onBg, bold = true, gapBefore = 18f, gapAfter = 14f)

                s.startsWith("> ") ->
                    text(s.removePrefix("> "), 40f, spec.muted, italic = true, indent = 32f, gapAfter = 12f)

                s.startsWith("- ") || s.startsWith("* ") ->
                    text("•  " + s.drop(2), 42f, spec.onBg, indent = 24f, gapAfter = 10f)

                BULLET.containsMatchIn(s) ->
                    text(s, 42f, spec.onBg, indent = 24f, gapAfter = 10f)

                else -> text(s, 42f, spec.onBg, gapAfter = 14f)
            }
        }
    }

    /* ---------- 行内标记 ---------- */

    private val BULLET = Regex("^\\d+\\.\\s")

    private val INLINE = Regex(
        "(\\*\\*(.+?)\\*\\*)" +                    // 1,2 粗体
            "|(`([^`]+?)`)" +                        // 3,4 行内代码
            "|(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)"      // 5 斜体
    )

    /** 剥掉标记，同时记下 span 该盖在哪一段上。 */
    private fun parseInline(src: String): Pair<String, List<Triple<Int, Int, Any>>> {
        val sb = StringBuilder()
        val spans = mutableListOf<Triple<Int, Int, Any>>()
        var last = 0

        INLINE.findAll(src).forEach { m ->
            if (m.range.first > last) sb.append(src, last, m.range.first)
            val start = sb.length
            val content: String
            val span: Any
            when {
                m.groupValues[1].isNotEmpty() -> {
                    content = m.groupValues[2]
                    span = StyleSpan(Typeface.BOLD)
                }
                m.groupValues[3].isNotEmpty() -> {
                    content = m.groupValues[4]
                    @Suppress("DEPRECATION")
                    span = TypefaceSpan("monospace")
                }
                else -> {
                    content = m.groupValues[5]
                    span = StyleSpan(Typeface.ITALIC)
                }
            }
            sb.append(content)
            if (sb.length > start) spans.add(Triple(start, sb.length, span))
            last = m.range.last + 1
        }
        if (last < src.length) sb.append(src, last, src.length)
        return sb.toString() to spans
    }

    /* ---------- 块 ---------- */

    private sealed class Block {
        abstract val gapBefore: Float
        abstract val gapAfter: Float
        abstract fun totalHeight(): Float

        class Text(
            val layout: StaticLayout,
            val indent: Float,
            override val gapBefore: Float,
            override val gapAfter: Float
        ) : Block() {
            override fun totalHeight(): Float = layout.height + gapBefore + gapAfter
        }

        class Rule(
            val thickness: Int,
            override val gapBefore: Float,
            override val gapAfter: Float
        ) : Block() {
            override fun totalHeight(): Float = thickness + gapBefore + gapAfter
        }

        /** 纯留白，用来撑开空行。 */
        class Gap(val height: Float) : Block() {
            override val gapBefore: Float = 0f
            override val gapAfter: Float = 0f
            override fun totalHeight(): Float = height
        }
    }

    /* ---------- 落地 ---------- */

    /** 存进系统相册的 Pictures/随笔。成功返回 true。 */
    fun saveToGallery(context: Context, bitmap: Bitmap, baseName: String): Boolean {
        return try {
            val fileName = "$baseName-${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/随笔")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri == null) {
                    false
                } else {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "随笔"
                )
                if (!dir.exists() && !dir.mkdirs()) {
                    false
                } else {
                    FileOutputStream(File(dir, fileName)).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /** 写缓存并换成 FileProvider Uri，用于分享。 */
    fun shareUri(context: Context, bitmap: Bitmap, baseName: String): Uri? = try {
        val dir = File(context.cacheDir, "share")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$baseName-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        null
    }

    /** 文件名里不能有这些东西。 */
    fun safeName(title: String): String {
        val t = title.trim().ifBlank { "无标题" }
        return t.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_").take(40)
    }
}