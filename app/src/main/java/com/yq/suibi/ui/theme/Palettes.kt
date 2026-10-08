package com.yq.suibi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 内置配色。色值来自公开开源配色项目：
 *  - Catppuccin       https://catppuccin.com/palette          (MIT)
 *  - Nord             https://www.nordtheme.com/docs/colors   (MIT)
 *  - Gruvbox          https://github.com/morhetz/gruvbox      (MIT)
 *  - Rosé Pine        https://rosepinetheme.com/palette       (MIT)
 *  - 宣纸 / 墨夜      本项目自带
 */
data class SuibiPalette(
    val id: String,
    val name: String,
    val author: String,
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val outline: Color
) {
    fun scheme(): ColorScheme {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = surfaceVariant,
            onPrimaryContainer = onBackground,
            secondary = primary,
            onSecondary = onPrimary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outline,
            error = if (dark) Color(0xFFF2B8B5) else Color(0xFFB3261E),
            onError = if (dark) Color(0xFF601410) else Color.White
        )
    }
}

private fun hex(v: Long) = Color(0xFF000000 or v)

object Palettes {

    val XuanZhi = SuibiPalette(
        id = "xuanzhi",
        name = "宣纸",
        author = "随笔",
        dark = false,
        background = hex(0xFAF7F2),
        surface = hex(0xFFFFFF),
        surfaceVariant = hex(0xF2EDE4),
        onBackground = hex(0x2B2721),
        onSurfaceVariant = hex(0x6B6459),
        primary = hex(0x8C6A4A),
        onPrimary = hex(0xFFFFFF),
        outline = hex(0xE3DACB)
    )

    val MoYe = SuibiPalette(
        id = "moye",
        name = "墨夜",
        author = "随笔",
        dark = true,
        background = hex(0x14120F),
        surface = hex(0x1E1B17),
        surfaceVariant = hex(0x26221D),
        onBackground = hex(0xEAE4DA),
        onSurfaceVariant = hex(0xA69E92),
        primary = hex(0xD8B98F),
        onPrimary = hex(0x3A2A18),
        outline = hex(0x332E28)
    )

    val Latte = SuibiPalette(
        id = "latte",
        name = "Catppuccin Latte",
        author = "Catppuccin",
        dark = false,
        background = hex(0xEFF1F5),
        surface = hex(0xFFFFFF),
        surfaceVariant = hex(0xE6E9EF),
        onBackground = hex(0x4C4F69),
        onSurfaceVariant = hex(0x6C6F85),
        primary = hex(0x8839EF),
        onPrimary = hex(0xFFFFFF),
        outline = hex(0xBCC0CC)
    )

    val Mocha = SuibiPalette(
        id = "mocha",
        name = "Catppuccin Mocha",
        author = "Catppuccin",
        dark = true,
        background = hex(0x1E1E2E),
        surface = hex(0x313244),
        surfaceVariant = hex(0x45475A),
        onBackground = hex(0xCDD6F4),
        onSurfaceVariant = hex(0xA6ADC8),
        primary = hex(0xCBA6F7),
        onPrimary = hex(0x1E1E2E),
        outline = hex(0x585B70)
    )

    val Nord = SuibiPalette(
        id = "nord",
        name = "Nord",
        author = "Arctic Ice Studio",
        dark = true,
        background = hex(0x2E3440),
        surface = hex(0x3B4252),
        surfaceVariant = hex(0x434C5E),
        onBackground = hex(0xD8DEE9),
        onSurfaceVariant = hex(0x8F9AAB),
        primary = hex(0x88C0D0),
        onPrimary = hex(0x2E3440),
        outline = hex(0x4C566A)
    )

    val Gruvbox = SuibiPalette(
        id = "gruvbox",
        name = "Gruvbox Dark",
        author = "morhetz",
        dark = true,
        background = hex(0x282828),
        surface = hex(0x3C3836),
        surfaceVariant = hex(0x504945),
        onBackground = hex(0xEBDBB2),
        onSurfaceVariant = hex(0xA89984),
        primary = hex(0xFE8019),
        onPrimary = hex(0x282828),
        outline = hex(0x665C54)
    )

    val RoseDawn = SuibiPalette(
        id = "rose-dawn",
        name = "Rosé Pine Dawn",
        author = "Rosé Pine",
        dark = false,
        background = hex(0xFAF4ED),
        surface = hex(0xFFFAF3),
        surfaceVariant = hex(0xF2E9E1),
        onBackground = hex(0x575279),
        onSurfaceVariant = hex(0x797593),
        primary = hex(0x907AA9),
        onPrimary = hex(0xFFFAF3),
        outline = hex(0xDFDAD9)
    )

    val RosePine = SuibiPalette(
        id = "rose-pine",
        name = "Rosé Pine",
        author = "Rosé Pine",
        dark = true,
        background = hex(0x191724),
        surface = hex(0x1F1D2E),
        surfaceVariant = hex(0x26233A),
        onBackground = hex(0xE0DEF4),
        onSurfaceVariant = hex(0x908CAA),
        primary = hex(0xC4A7E7),
        onPrimary = hex(0x191724),
        outline = hex(0x524F67)
    )

    val all: List<SuibiPalette> = listOf(
        XuanZhi, MoYe, Latte, Mocha, Nord, Gruvbox, RoseDawn, RosePine
    )

    fun byId(id: String?): SuibiPalette =
        all.firstOrNull { it.id == id } ?: XuanZhi
}

/**
 * 标记底色。把主题的 surface 与用户选的颜色按比例混合，
 * 得到一层淡淡的底色，不是整块高饱和色。
 */
fun tintSurface(base: Color, accent: Color, dark: Boolean): Color {
    val amount = if (dark) 0.20f else 0.14f
    return Color(
        red = base.red + (accent.red - base.red) * amount,
        green = base.green + (accent.green - base.green) * amount,
        blue = base.blue + (accent.blue - base.blue) * amount,
        alpha = 1f
    )
}

/** 预设标记色（Radix Colors / MIT）。 */
val MarkColors: List<Color> = listOf(
    hex(0xE5484D), // 红
    hex(0xF76B15), // 橙
    hex(0xFFB224), // 琥珀
    hex(0x99D52A), // 黄绿
    hex(0x30A46C), // 绿
    hex(0x12A594), // 青
    hex(0x0091FF), // 天蓝
    hex(0x3E63DD), // 蓝
    hex(0x8E4EC6), // 紫
    hex(0xD6409F), // 品红
    hex(0xE93D82), // 粉
    hex(0xAD7F58), // 棕
    hex(0x8B8D98)  // 灰
)