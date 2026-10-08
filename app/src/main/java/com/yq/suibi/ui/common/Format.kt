package com.yq.suibi.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val fmtTime = SimpleDateFormat("HH:mm", Locale.getDefault())
private val fmtMD = SimpleDateFormat("MM/dd", Locale.getDefault())
private val fmtYMD = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

/** 相对时间：刚刚 / 12 分钟前 / 今天 14:30 / 昨天 14:30 / 10/24 / 2023/05/01 */
fun relativeTime(ts: Long): String {
    if (ts <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - ts
    if (diff < 60_000L) return "刚刚"
    if (diff < 3_600_000L) return "${diff / 60_000L} 分钟前"

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return when {
        ts >= today -> "今天 ${fmtTime.format(Date(ts))}"
        ts >= today - 86_400_000L -> "昨天 ${fmtTime.format(Date(ts))}"
        ts >= today - 86_400_000L * 330 -> fmtMD.format(Date(ts))
        else -> fmtYMD.format(Date(ts))
    }
}

fun absoluteTime(ts: Long): String =
    if (ts <= 0L) "" else "${fmtYMD.format(Date(ts))} ${fmtTime.format(Date(ts))}"

/** 列表里的正文摘要：合并空白 + 截断。 */
fun snippet(text: String, max: Int = 70): String {
    val flat = text.replace(Regex("\\s+"), " ").trim()
    return if (flat.length <= max) flat else flat.take(max) + "…"
}