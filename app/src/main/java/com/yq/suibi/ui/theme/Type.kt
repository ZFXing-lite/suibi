package com.yq.suibi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 按倍率缩放整套字号。1.0 = 标准。 */
fun suibiTypography(scale: Float): Typography {
    val s = scale.coerceIn(0.8f, 1.4f)
    fun TextStyle.scaled() = copy(fontSize = fontSize * s, lineHeight = lineHeight * s)

    return Typography(
        titleLarge = SuibiTypography.titleLarge.scaled(),
        titleMedium = SuibiTypography.titleMedium.scaled(),
        titleSmall = SuibiTypography.titleSmall.scaled(),
        bodyLarge = SuibiTypography.bodyLarge.scaled(),
        bodyMedium = SuibiTypography.bodyMedium.scaled(),
        bodySmall = SuibiTypography.bodySmall.scaled(),
        labelLarge = SuibiTypography.labelLarge.scaled(),
        labelSmall = SuibiTypography.labelSmall.scaled()
    )
}

val SuibiTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)