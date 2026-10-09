package com.yq.suibi.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 富文本渲染的坐标映射测试。
 *
 * 这是整个功能里最容易出错、又最不方便手动验证的部分 ——
 * 映射错了的表现是「光标乱跳」，在模拟器里点半天也未必复现。
 * 所以把纯逻辑抽出来在这里钉死。
 */
class RichTextTransformTest {

    private fun transform(cursorOffset: Int = 0, text: String) = RichTextTransform(
        ranges = emptyList(),
        currentIndex = 0,
        hitNormal = Color.Transparent,
        hitActive = Color.Transparent,
        cursorLine = lineRangeOf(text, cursorOffset),
        accent = Color.Unspecified,
        muted = Color.Unspecified,
        codeBg = Color.Unspecified,
        codeFg = Color.Unspecified
    )

    /** 光标放在末尾，等于「没有哪一行要显露标记」的反面 —— 末行会显露，所以用第一行测隐藏。 */
    private fun hidden(text: String) = transform(cursorOffset = text.length, text = text)

    private fun shown(text: String, offset: Int) = transform(cursorOffset = offset, text = text)

    private fun display(text: String, cursorOffset: Int = text.length): String =
        transform(cursorOffset, text).filter(AnnotatedString(text)).text.text

    /* ---------- 基本渲染 ---------- */

    @Test
    fun `粗体标记被隐藏，内容保留`() {
        // 光标在第二行，第一行的标记应当隐藏
        val t = "**粗体**\n尾"
        assertEquals("粗体\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `斜体和行内代码`() {
        val t = "*斜* 和 `码`\n尾"
        assertEquals("斜 和 码\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `标题标记被隐藏`() {
        val t = "## 标题\n尾"
        assertEquals("标题\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `无序列表的点被换掉`() {
        val t = "- 一条\n尾"
        assertEquals("\u2022 一条\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `引用标记被隐藏`() {
        val t = "> 引用\n尾"
        assertEquals("引用\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `分隔线换成横线`() {
        val t = "---\n尾"
        assertEquals("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n尾", display(t, cursorOffset = t.length))
    }

    @Test
    fun `有序列表的数字保留`() {
        val t = "1. 第一\n尾"
        assertEquals("1. 第一\n尾", display(t, cursorOffset = t.length))
    }

    /* ---------- 光标行显露标记 ---------- */

    @Test
    fun `光标所在行的标记原样显示`() {
        val t = "**粗体**"
        assertEquals("**粗体**", display(t, cursorOffset = 0))
    }

    @Test
    fun `只有光标那一行显露，别的行照旧隐藏`() {
        val t = "**甲**\n**乙**"
        // 光标在第一行
        assertEquals("**甲**\n乙", display(t, cursorOffset = 2))
        // 光标在第二行
        assertEquals("甲\n**乙**", display(t, cursorOffset = 8))
    }

    @Test
    fun `显露时长度不变，映射是恒等的`() {
        val t = "**粗体**"
        val tt = shown(t, 0).filter(AnnotatedString(t))
        assertEquals(t, tt.text.text)
        for (i in 0..t.length) {
            assertEquals(i, tt.text.text.let { tt.offsetMapping.originalToTransformed(i) })
            assertEquals(i, tt.offsetMapping.transformedToOriginal(i))
        }
    }

    /* ---------- 映射正确性 ---------- */

    @Test
    fun `隐藏标记后长度差等于被吃掉的字符数`() {
        val t = "**粗体**\n尾"
        val out = transform(t.length, t).filter(AnnotatedString(t))
        assertEquals(4, t.length - out.text.text.length)   // 吃掉了 4 个 *
    }

    @Test
    fun `原文偏移在隐藏区间内时收敛到该段的显示起点`() {
        val t = "**粗体**"
        // 光标放最后一行 -> 标记全部隐藏。用末尾以外的行来测不行，
        // 所以换成两行结构，第一行隐藏。
        val s = "**粗体**\n后"
        val out = transform(s.length, s).filter(AnnotatedString(s))

        // 原文 0、1 是 "**"，都落在被隐藏的前缀里 -> 都映射到显示位置 0
        assertEquals(0, out.offsetMapping.originalToTransformed(0))
        assertEquals(0, out.offsetMapping.originalToTransformed(1))
        // 原文 2 是「粗」 -> 显示位置 0
        assertEquals(0, out.offsetMapping.originalToTransformed(2))
        // 原文 4 是「体」之后 -> 显示位置 2
        assertEquals(2, out.offsetMapping.originalToTransformed(4))
        // 原文 6 越过结尾 "**" -> 显示位置 2
        assertEquals(2, out.offsetMapping.originalToTransformed(6))
    }

    @Test
    fun `反向映射落在合法原文范围`() {
        val s = "**粗体**\n后"
        val out = transform(s.length, s).filter(AnnotatedString(s))
        val n = out.text.text.length
        for (q in 0..n) {
            val r = out.offsetMapping.transformedToOriginal(q)
            assertTrue("显示偏移 $q 映射到越界的原文偏移 $r", r in 0..s.length)
        }
    }

    @Test
    fun `正向映射单调不减`() {
        val s = "**粗**\n# 标题\n- 点\n> 引\n`码`\n尾"
        val out = transform(s.length, s).filter(AnnotatedString(s))
        var prev = -1
        for (r in 0..s.length) {
            val d = out.offsetMapping.originalToTransformed(r)
            assertTrue("原文 $r 映射回退了：$d < $prev", d >= prev)
            prev = d
        }
    }

    @Test
    fun `反向映射单调不减`() {
        val s = "**粗**\n# 标题\n- 点\n> 引\n`码`\n尾"
        val out = transform(s.length, s).filter(AnnotatedString(s))
        var prev = -1
        for (q in 0..out.text.text.length) {
            val r = out.offsetMapping.transformedToOriginal(q)
            assertTrue("显示 $q 映射回退了：$r < $prev", r >= prev)
            prev = r
        }
    }

    @Test
    fun `映射两端对齐`() {
        val s = "**粗**\n# 标题\n- 点\n尾"
        val out = transform(s.length, s).filter(AnnotatedString(s))
        assertEquals(0, out.offsetMapping.originalToTransformed(0))
        assertEquals(
            out.text.text.length,
            out.offsetMapping.originalToTransformed(s.length)
        )
        assertEquals(0, out.offsetMapping.transformedToOriginal(0))
        assertEquals(
            s.length,
            out.offsetMapping.transformedToOriginal(out.text.text.length)
        )
    }

    /* ---------- 边角 ---------- */

    @Test
    fun `空文本不炸`() {
        val out = transform(0, "").filter(AnnotatedString(""))
        assertEquals("", out.text.text)
        assertEquals(0, out.offsetMapping.originalToTransformed(0))
        assertEquals(0, out.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `只有标记没有内容的半截串不被吃掉`() {
        assertEquals("**", display("**", 0))
        assertEquals("**", display("**", 2))
    }

    @Test
    fun `未闭合的标记原样保留`() {
        val t = "**没闭合\n尾"
        assertEquals("**没闭合\n尾", display(t, t.length))
    }

    @Test
    fun `下划线不当作斜体`() {
        val t = "a_b_c\n尾"
        assertEquals("a_b_c\n尾", display(t, t.length))
    }

    @Test
    fun `三星号按粗体处理，不残留星号`() {
        val t = "***粗斜***\n尾"
        val d = display(t, t.length)
        assertTrue("不该残留星号：$d", !d.contains('*'))
    }

    @Test
    fun `多重标记混在一行`() {
        val t = "# **粗** 与 `码`\n尾"
        val d = display(t, t.length)
        assertEquals("粗 与 码\n尾", d)
    }

    @Test
    fun `越界的偏移被夹住不抛异常`() {
        val t = "**粗**"
        val out = transform(0, t).filter(AnnotatedString(t))
        assertEquals(0, out.offsetMapping.originalToTransformed(-5))
        assertEquals(t.length, out.offsetMapping.originalToTransformed(999))
        assertEquals(0, out.offsetMapping.transformedToOriginal(-5))
        assertEquals(t.length, out.offsetMapping.transformedToOriginal(999))
    }

    /* ---------- 样式确实贴上了 ---------- */

    @Test
    fun `粗体内容真的带了 Bold 样式`() {
        val t = "**粗体**\n尾"
        val out = transform(t.length, t).filter(AnnotatedString(t))
        val spans = out.text.spanStyles
        val bold = spans.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("没有找到 Bold 样式", bold.isNotEmpty())
        // 命中范围应覆盖显示文本里的「粗体」(偏移 0..2)
        assertTrue(bold.any { it.start == 0 && it.end == 2 })
    }

    @Test
    fun `搜索底色贴在正确的显示位置`() {
        val t = "**粗体**\n尾"
        val tt = RichTextTransform(
            ranges = listOf(2 until 4),   // 原文里的「粗体」
            currentIndex = 0,
            hitNormal = Color.Red,
            hitActive = Color.Red,
            cursorLine = lineRangeOf(t, t.length),
            accent = Color.Unspecified,
            muted = Color.Unspecified,
            codeBg = Color.Unspecified,
            codeFg = Color.Unspecified
        )
        val out = tt.filter(AnnotatedString(t))
        val hit = out.text.spanStyles.filter { it.item.background == Color.Red }
        assertEquals(1, hit.size)
        assertEquals(0, hit[0].start)
        assertEquals(2, hit[0].end)
    }

    /* ---------- lineRangeOf ---------- */

    @Test
    fun `lineRangeOf 取到光标所在行`() {
        val t = "abc\ndefg\nhi"
        assertEquals(0 until 3, lineRangeOf(t, 0))
        assertEquals(0 until 3, lineRangeOf(t, 2))
        assertEquals(4 until 8, lineRangeOf(t, 5))
        assertEquals(9 until 11, lineRangeOf(t, 11))
        // 末尾越界
        assertEquals(9 until 11, lineRangeOf(t, 99))
    }

    @Test
    fun `lineRangeOf 在换行符上归属前一行`() {
        val t = "abc\ndef"
        // 偏移 3 是那个 '\n'，它属于第一行
        assertEquals(0 until 3, lineRangeOf(t, 3))
    }
}