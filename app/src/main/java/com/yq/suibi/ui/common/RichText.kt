package com.yq.suibi.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

/**
 * 正文的富文本渲染。
 *
 * 核心是一层 [VisualTransformation]：把 `**粗体**` 这类标记从显示里拿掉，
 * 换成真的加粗；标题放大、引用变色、代码换成等宽加底色。
 * 原文一个字节都不改 —— 存进数据库的仍是带标记的纯文本，
 * 导出图片、搜索、备份全都照旧。
 *
 * 光标所在的那一行是例外：标记原样显示（只是压暗），
 * 否则你没法把光标挪到 `**` 中间去删它。这是 Typora / Obsidian 的做法。
 *
 * ## 为什么敢动 OffsetMapping
 *
 * 显示的字符比原文少，所以「显示位置 ↔ 原文位置」必须显式映射，
 * 映射错了光标就会乱跳。这里用「片段」模型把映射算出来：
 * 每一段原文 [rawStart, rawEnd) 对应一段显示文本 display，
 * 两边的长度都已知，映射就是逐段累加，不靠启发式。
 * 全部行为有单元测试盯着，见 `app/src/test/.../RichTextTransformTest.kt`。
 */
class RichTextTransform(
    // 搜索命中，坐标在原文里
    private val ranges: List<IntRange>,
    private val currentIndex: Int,
    private val hitNormal: Color,
    private val hitActive: Color,
    // 光标所在行，这一行的标记不隐藏
    private val cursorLine: IntRange,
    private val accent: Color,
    private val muted: Color,
    private val codeBg: Color,
    private val codeFg: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val pieces = parse(raw, cursorLine)

        // 累加出显示文本 + 正向映射（原文偏移 -> 显示偏移）
        val sb = StringBuilder(raw.length)
        val rawToDisplay = IntArray(raw.length + 1)

        for (p in pieces) {
            val n = p.rawEnd - p.rawStart
            val m = p.display.length
            val d0 = sb.length

            for (i in 0 until n) {
                // 段内原文比显示长（标记被吃掉）时，整段收敛到显示起点
                rawToDisplay[p.rawStart + i] = if (n == m) d0 + i else d0
            }
            sb.append(p.display)
        }
        rawToDisplay[raw.length] = sb.length

        val display = sb.toString()

        // 反向映射取正向映射的「左逆」：每个显示位置记第一个映射到它的原文偏移。
        // 这样开头落在原文 0、结尾落在原文末尾，光标不会因为标记被隐藏而漂移；
        // 而且它天然是单调不减的（正向映射单调，左逆就单调）。
        val displayToRaw = IntArray(display.length + 1)
        var next = 0
        for (r in 0..raw.length) {
            val d = rawToDisplay[r]
            while (next <= d && next < display.length) {
                displayToRaw[next] = r
                next++
            }
        }
        displayToRaw[display.length] = raw.length

        // 把样式贴到显示文本上：片段的样式 + 搜索命中底色
        val out = AnnotatedString.Builder(display)
        var d = 0
        for (p in pieces) {
            if (p.style != null && p.display.isNotEmpty()) {
                out.addStyle(p.style, d, d + p.display.length)
            }
            d += p.display.length
        }

        ranges.forEachIndexed { i, r ->
            if (r.first < 0 || r.last >= raw.length) return@forEachIndexed
            val s = rawToDisplay[r.first]
            val e = rawToDisplay[r.last + 1]
            if (e > s) {
                out.addStyle(
                    SpanStyle(background = if (i == currentIndex) hitActive else hitNormal),
                    s,
                    e
                )
            }
        }

        return TransformedText(out.toAnnotatedString(), Mapper(rawToDisplay, displayToRaw))
    }

    /** 纯查表，没有逻辑。 */
    private class Mapper(
        private val rawToDisplay: IntArray,
        private val displayToRaw: IntArray
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            rawToDisplay[offset.coerceIn(0, rawToDisplay.size - 1)]

        override fun transformedToOriginal(offset: Int): Int =
            displayToRaw[offset.coerceIn(0, displayToRaw.size - 1)]
    }

    /* ================= 解析 ================= */

    /**
     * 原文的一段。
     *
     * [display] 是它在显示文本里的样子，长度可以和原文长度不同；
     * 映射按「整段」算，所以段内不需要再细分。
     */
    private class Piece(
        val rawStart: Int,
        val rawEnd: Int,
        val display: String,
        val style: SpanStyle? = null
    )

    private fun parse(raw: String, cursorLine: IntRange): List<Piece> {
        val out = ArrayList<Piece>(raw.length)
        if (raw.isEmpty()) return out

        var ls = 0
        while (ls <= raw.length) {
            val nl = raw.indexOf('\n', ls)
            val le = if (nl == -1) raw.length else nl

            val reveal = ls <= cursorLine.first && cursorLine.first <= le

            emitLine(raw, ls, le, reveal, out)

            if (nl == -1) break

            // 换行本身原样带过去
            out.add(Piece(nl, nl + 1, "\n"))
            ls = nl + 1
        }
        return out
    }

    private fun emitLine(raw: String, ls: Int, le: Int, reveal: Boolean, out: MutableList<Piece>) {
        if (ls == le) return

        val line = raw.substring(ls, le)

        // 分隔线：整行换成一条横线
        if (DIVIDER.matches(line)) {
            out.add(Piece(ls, le, RULE, SpanStyle(color = muted)))
            return
        }

        // 标题：## 到 ###
        HEADING.find(line)?.let { m ->
            val hashes = m.range.last - m.range.first + 1
            val markerEnd = ls + m.range.last + 1
            out.add(marker(ls, markerEnd, reveal, raw))
            val size = when (hashes) {
                1 -> 1.5f
                2 -> 1.28f
                else -> 1.13f
            }
            // 标题里也可能有 **粗体** 或 `代码`，所以照样走行内解析
            inline(
                raw, markerEnd, le, reveal,
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = size.em, color = accent),
                out
            )
            return
        }

        // 引用：> 开头
        val qt = quotePrefix(line)
        if (qt > 0) {
            val markerEnd = ls + qt
            out.add(marker(ls, markerEnd, reveal, raw))
            inline(raw, markerEnd, le, reveal, SpanStyle(color = muted, fontStyle = FontStyle.Italic), out)
            return
        }

        // 无序列表：- * + ，显示成实心点
        val bl = bulletPrefix(line)
        if (bl > 0) {
            val markerEnd = ls + bl
            val bullet = if (reveal) raw.substring(ls, markerEnd) else "$BULLET "
            out.add(Piece(ls, markerEnd, bullet, SpanStyle(color = accent, fontWeight = FontWeight.Bold)))
            inline(raw, markerEnd, le, reveal, null, out)
            return
        }

        // 有序列表：数字加点，标记保留但染色
        val ol = orderedPrefix(line)
        if (ol > 0) {
            val markerEnd = ls + ol
            out.add(
                Piece(
                    ls, markerEnd, raw.substring(ls, markerEnd),
                    SpanStyle(color = accent, fontWeight = FontWeight.Bold)
                )
            )
            inline(raw, markerEnd, le, reveal, null, out)
            return
        }

        inline(raw, ls, le, reveal, null, out)
    }

    /** 一个行首标记：隐藏时显示为空串，显露时原样显示但压暗。 */
    private fun marker(ls: Int, markerEnd: Int, reveal: Boolean, raw: String): Piece =
        Piece(
            ls, markerEnd,
            if (reveal) raw.substring(ls, markerEnd) else "",
            SpanStyle(color = muted)
        )

    /**
     * 叠加一个行内样式。
     *
     * 行内代码自带底色和前色，直接覆盖外层；粗体斜体只补字号之外的字形，
     * 颜色沿用外层（比如引用里的粗体也该是引用色）。
     * SpanStyle 的字段都是非空类型，所以只能中性地声明，不能传 null。
     */
    private fun buildSpan(bold: Boolean, italic: Boolean, code: Boolean, base: SpanStyle?): SpanStyle {
        val fg = if (code) codeFg else (base?.color ?: Color.Unspecified)
        val bg = if (code) codeBg else (base?.background ?: Color.Unspecified)
        return SpanStyle(
            color = fg,
            background = bg,
            // 字号必须继承，否则标题里的粗体会掉回正文大小
            fontSize = base?.fontSize ?: TextUnit.Unspecified,
            fontWeight = if (bold) FontWeight.Bold else base?.fontWeight,
            fontStyle = if (italic) FontStyle.Italic else base?.fontStyle,
            fontFamily = if (code) FontFamily.Monospace else base?.fontFamily
        )
    }

    /** 行内标记：粗体、斜体、行内代码。 */
    private fun inline(
        raw: String,
        from: Int,
        to: Int,
        reveal: Boolean,
        base: SpanStyle?,
        out: MutableList<Piece>
    ) {
        var i = from
        var plainStart = from

        fun flushPlain(endExclusive: Int) {
            if (endExclusive > plainStart) {
                out.add(Piece(plainStart, endExclusive, raw.substring(plainStart, endExclusive), base))
            }
        }

        while (i < to) {
            val hit = matchAt(raw, i, to)
            if (hit == null) { i++; continue }

            flushPlain(i)
            val (token, contentStart, contentEnd) = hit
            val markerEnd = contentStart
            val closeEnd = contentEnd + token.length

            out.add(marker(i, markerEnd, reveal, raw))

            // 显式传 null 表示「不覆盖」，否则外层样式会被内层抹掉
            val isCode = token == "`"
            val inner = buildSpan(
                bold = token.length >= 2,
                italic = token == "*",
                code = isCode,
                base = base
            )
            out.add(Piece(contentStart, contentEnd, raw.substring(contentStart, contentEnd), inner))
            out.add(marker(contentEnd, closeEnd, reveal, raw))

            i = closeEnd
            plainStart = i
        }

        flushPlain(to)
    }

    /**
     * 在 [at] 处尝试匹配一个行内标记，返回 (标记串, 内容起点, 内容终点)。
     * 内容不能为空，也不能跨行。
     */
    private fun matchAt(raw: String, at: Int, to: Int): Triple<String, Int, Int>? {
        for (token in INLINE_TOKENS) {
            if (!raw.startsWith(token, at)) continue
            val contentStart = at + token.length
            val close = raw.indexOf(token, contentStart)
            if (close < 0 || close + token.length > to) continue
            // 空内容不算，否则 `**` 这种半截标记会被吃掉
            if (close == contentStart) continue
            return Triple(token, contentStart, close)
        }
        return null
    }

    private companion object {
        /** 从长到短，保证 *** 先于 * 命中 */
        val INLINE_TOKENS = listOf("***", "**", "*", "`")

        val HEADING = Regex("^#{1,3} ")
        val DIVIDER = Regex("^\\s*---\\s*$")
        val BULLET = '\u2022'   // •

        /** 替换分隔线的那条横线 */
        const val RULE = "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"

        fun quotePrefix(line: String): Int {
            var i = 0
            while (i < line.length && line[i] == ' ') i++
            if (i < line.length && line[i] == '>') {
                i++
                if (i < line.length && line[i] == ' ') i++
                return i
            }
            return 0
        }

        fun bulletPrefix(line: String): Int {
            var i = 0
            while (i < line.length && line[i] == ' ') i++
            if (i + 1 < line.length && (line[i] == '-' || line[i] == '*' || line[i] == '+')
                && line[i + 1] == ' '
            ) {
                return i + 2
            }
            return 0
        }

        fun orderedPrefix(line: String): Int {
            var i = 0
            while (i < line.length && line[i] == ' ') i++
            val digits = i
            while (i < line.length && line[i].isDigit()) i++
            if (i > digits && i + 1 < line.length && line[i] == '.' && line[i + 1] == ' ') {
                return i + 2
            }
            return 0
        }
    }
}

/** 光标在第几行（按原文算）。 */
fun lineRangeOf(text: String, offset: Int): IntRange {
    val o = offset.coerceIn(0, text.length)
    val s = if (o == 0) 0 else {
        val idx = text.lastIndexOf('\n', o - 1)
        if (idx == -1) 0 else idx + 1
    }
    val e = text.indexOf('\n', o).let { if (it == -1) text.length else it }
    return s until e
}