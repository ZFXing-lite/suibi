package com.yq.suibi.ui.common

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

// 命中高亮原本是单独一个 HighlightTransform，只加底色不改字符，所以能偷懒用
// OffsetMapping.Identity。v1.5 起富文本渲染要删掉标记字符，映射必须真算，
// 于是两者合并进 RichTextTransform —— 见 RichText.kt。