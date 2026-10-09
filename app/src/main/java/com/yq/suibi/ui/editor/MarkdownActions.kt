package com.yq.suibi.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Markdown 语法插入。所有操作都在 TextFieldValue 上做，
 * 保证光标/选区在插入后落在合理位置。
 *
 * 两个「开关」概念：
 * - [wrap] 是 toggle 的：选区内已带标记就摘掉，否则加上。
 *   否则连按两次 B 会得到 `****文字****`。
 * - [prefixLine] 也是 toggle 的：行首已有该前缀就删掉。
 */
object Md {

    /** 包裹型开关：**粗体**、*斜体*、`代码` */
    fun wrap(value: TextFieldValue, token: String): TextFieldValue {
        val s = value.selection.min
        val e = value.selection.max
        val selected = value.text.substring(s, e)

        // 选中的文字本身带着标记 -> 摘掉
        if (selected.length >= token.length * 2 &&
            selected.startsWith(token) && selected.endsWith(token)
        ) {
            val inner = selected.substring(token.length, selected.length - token.length)
            val newText = value.text.replaceRange(s, e, inner)
            return value.copy(text = newText, selection = TextRange(s, s + inner.length))
        }

        // 选区外面紧贴着标记 -> 摘掉
        if (s >= token.length && e + token.length <= value.text.length &&
            value.text.substring(s - token.length, s) == token &&
            value.text.substring(e, e + token.length) == token
        ) {
            val newText = value.text.removeRange(e, e + token.length)
                .removeRange(s - token.length, s)
            return value.copy(
                text = newText,
                selection = TextRange(s - token.length, e - token.length)
            )
        }

        val newText = value.text.replaceRange(s, e, "$token$selected$token")
        val cursor = if (selected.isEmpty()) s + token.length
        else s + token.length + selected.length + token.length
        return value.copy(
            text = newText,
            selection = TextRange(cursor)
        )
    }

    /** 行首前缀开关：## 标题、- 列表、> 引用、1. 有序 */
    fun prefixLine(value: TextFieldValue, prefix: String): TextFieldValue {
        val s = value.selection.min
        val lineStart = if (s == 0) 0 else {
            val idx = value.text.lastIndexOf('\n', s - 1)
            if (idx == -1) 0 else idx + 1
        }

        // 已经有这个前缀 -> 去掉（切换语义）
        if (value.text.startsWith(prefix, lineStart)) {
            val newText = value.text.removeRange(lineStart, lineStart + prefix.length)
            return value.copy(
                text = newText,
                selection = TextRange((s - prefix.length).coerceAtLeast(lineStart))
            )
        }

        val newText = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart)
        return value.copy(
            text = newText,
            selection = TextRange(s + prefix.length)
        )
    }

    /**
     * 光标/选区处是否已经有这个包裹标记。
     * 只看紧贴选区外侧的字符，简单但够用。
     */
    fun isWrapped(text: String, selStart: Int, selEnd: Int, token: String): Boolean {
        val s = selStart.coerceIn(0, text.length)
        val e = selEnd.coerceIn(s, text.length)
        if (e > s) {
            return text.substring(s, e).let { it.startsWith(token) && it.endsWith(token) && it.length >= token.length * 2 }
        }
        // 光标在标记紧后面，或选区外紧贴标记
        return s >= token.length && text.startsWith(token, s - token.length) ||
            e + token.length <= text.length && text.startsWith(token, e)
    }

    /** 在光标处插入分隔线，前后补换行。 */
    fun insertBlock(value: TextFieldValue, block: String): TextFieldValue {
        val s = value.selection.min
        val e = value.selection.max
        val before = value.text.substring(0, s)
        val after = value.text.substring(e)
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val tail = if (after.isEmpty() || after.startsWith("\n")) "\n" else "\n\n"
        val insert = "$lead$block$tail"
        return value.copy(
            text = before + insert + after,
            selection = TextRange(s + insert.length)
        )
    }

    /** 在光标处插入换行 + 固定缩进，用于续写列表。 */
    fun newline(value: TextFieldValue): TextFieldValue {
        val s = value.selection.min
        val e = value.selection.max
        val newText = value.text.replaceRange(s, e, "\n")
        return value.copy(text = newText, selection = TextRange(s + 1))
    }
}

/** 取光标所在的那一行（不含换行符）。给工具栏判断当前行前缀用。 */
fun lineAt(text: String, offset: Int): String {
    val o = offset.coerceIn(0, text.length)
    val s = if (o == 0) 0 else {
        val idx = text.lastIndexOf('\n', o - 1)
        if (idx == -1) 0 else idx + 1
    }
    val e = text.indexOf('\n', o).let { if (it == -1) text.length else it }
    return text.substring(s, e)
}