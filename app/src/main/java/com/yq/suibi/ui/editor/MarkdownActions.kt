package com.yq.suibi.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Markdown 语法插入。所有操作都在 TextFieldValue 上做，
 * 保证光标/选区在插入后落在合理位置。
 */
object Md {

    /** 包裹型：**粗体**、*斜体*、`代码` */
    fun wrap(value: TextFieldValue, token: String): TextFieldValue {
        val s = value.selection.min
        val e = value.selection.max
        val selected = value.text.substring(s, e)
        val newText = value.text.replaceRange(s, e, "$token$selected$token")
        val cursor = if (selected.isEmpty()) s + token.length
        else s + token.length + selected.length + token.length
        return value.copy(
            text = newText,
            selection = TextRange(cursor)
        )
    }

    /** 行首前缀：## 标题、- 列表、> 引用、1. 有序 */
    fun prefixLine(value: TextFieldValue, prefix: String): TextFieldValue {
        val s = value.selection.min
        val lineStart = if (s == 0) 0 else {
            val idx = value.text.lastIndexOf('\n', s - 1)
            if (idx == -1) 0 else idx + 1
        }
        val newText = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart)
        return value.copy(
            text = newText,
            selection = TextRange(s + prefix.length)
        )
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