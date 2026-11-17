package com.ghdinteractivestudio.jobhunter.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Whether the dark half of each color pair applies; JobHunterTheme sets it from the user toggle. */
val LocalDark = staticCompositionLocalOf { true }

private fun hex(value: Long) = Color(0xFF000000 or value)

@Composable
@ReadOnlyComposable
private fun pair(light: Long, dark: Long) = hex(if (LocalDark.current) dark else light)

/**
 * CodeStation Signature Liquid Glassmorphism design palette:
 * - Dark Mode: Deep Midnight Obsidian (#0B0D12) with smoked glass & luminous neon accents
 * - Light Mode: Frosted Crystal Mesh (#E7EFF8) with translucent white glass & high-contrast ink/deep-emerald accents
 */
object Palette {
    val background: Color @Composable @ReadOnlyComposable get() = pair(0xE9F0F8, 0x0B0D12)
    val surface: Color @Composable @ReadOnlyComposable get() = pair(0xF3F7FC, 0x171B24)
    val cardSurface: Color @Composable @ReadOnlyComposable get() = pair(0xFFFFFF, 0x1A202C)
    val border: Color @Composable @ReadOnlyComposable get() = pair(0xCBD5E1, 0x262D3D)
    val text: Color @Composable @ReadOnlyComposable get() = pair(0x0F172A, 0xF9FAFB)
    val secondary: Color @Composable @ReadOnlyComposable get() = pair(0x334155, 0x9CA3AF)
    val tertiary: Color @Composable @ReadOnlyComposable get() = pair(0x475569, 0x6B7280)

    val accentFill: Color @Composable @ReadOnlyComposable get() = pair(0x0F172A, 0xFFFFFF)
    val accent: Color @Composable @ReadOnlyComposable get() = pair(0x0F172A, 0xFFFFFF)
    val onAccent: Color @Composable @ReadOnlyComposable get() = pair(0xFFFFFF, 0x0B0D12)
    val accentDim: Color @Composable @ReadOnlyComposable get() = pair(0xCBD5E1, 0x374151)

    val danger: Color @Composable @ReadOnlyComposable get() = pair(0xB91C1C, 0xF87171)
    val warning: Color @Composable @ReadOnlyComposable get() = pair(0xB45309, 0xF0A030)
    val attention: Color @Composable @ReadOnlyComposable get() = pair(0x92400E, 0xFBBF24)
    val codeBackground: Color @Composable @ReadOnlyComposable get() = pair(0xE2E8F0, 0x0E1117)
    val added: Color @Composable @ReadOnlyComposable get() = pair(0x047857, 0x34D399)

    // Glassmorphism design tokens
    val glassSurface: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0xD9171B24) else Color(0xBFFFFFFF)
    val glassCard: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0xB3171B24) else Color(0xB0FFFFFF)
    val glassBorder: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x38FFFFFF) else Color(0x330F172A)
    val glassDivider: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x1FFFFFFF) else Color(0x140F172A)
    val plateBackground: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x2B000000) else Color(0x66E2E8F0)

    // Electric Sapphire Blue (high contrast in Light & Dark)
    val electricBlue: Color @Composable @ReadOnlyComposable get() = pair(0x1D4ED8, 0x60A5FA)
    val blueSolidBg: Color @Composable @ReadOnlyComposable get() = pair(0x1D4ED8, 0x2563EB)
    val electricBlueSoft: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x28438DFF) else Color(0x1A1D4ED8)
    val cyanNeon: Color @Composable @ReadOnlyComposable get() = pair(0x0284C7, 0x00D2FF)
    val turquoise: Color @Composable @ReadOnlyComposable get() = pair(0x0F766E, 0x2DD4BF)

    // Emerald Accents & Accessible Solid Fills (Light: #047857, Dark text/icon: #10B981, Dark solid fill: #059669 -> crisp white legibility!)
    val emeraldAccent: Color @Composable @ReadOnlyComposable get() = pair(0x047857, 0x10B981)
    val emeraldSolidBg: Color @Composable @ReadOnlyComposable get() = pair(0x047857, 0x059669)
    val emeraldGlow: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x2610B981) else Color(0x1C047857)
    val onEmerald: Color get() = Color.White
}
