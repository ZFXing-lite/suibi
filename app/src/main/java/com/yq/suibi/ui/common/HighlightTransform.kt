package com.yq.suibi.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** 在纯文本里找出全部匹配。大小写不敏感，逐个不重叠地推进。 */
fun findMatches(text: String, query: String): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    var i = text.indexOf(query, 0, ignoreCase = true)
    while (i >= 0) {
        out.add(i until i + query.length)
        i = text.indexOf(query, i + query.length, ignoreCase = true)
    }
    return out
}

/**
 * 只给命中处加底色，不动字符本身 —— 所以 OffsetMapping 是恒等的，
 * 光标位置、选区、输入法都不受影响。
 *
 * ranges 由调用方算好传进来：在 filter 里做副作用会踩到组合期。
 */
class HighlightTransform(
    private val ranges: List<IntRange>,
    private val currentIndex: Int,
    private val normal: Color,
    private val active: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (ranges.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val b = AnnotatedString.Builder(text)
        ranges.forEachIndexed { i, r ->
            if (r.first < 0 || r.last >= text.length) return@forEachIndexed
            b.addStyle(
                SpanStyle(background = if (i == currentIndex) active else normal),
                r.first,
                r.last + 1
            )
        }
        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }
}