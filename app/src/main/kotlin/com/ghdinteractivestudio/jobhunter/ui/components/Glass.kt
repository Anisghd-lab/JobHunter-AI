package com.ghdinteractivestudio.jobhunter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Motion
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold

@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = Motion.press(),
        label = "Press"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberPress(): Pair<MutableInteractionSource, Boolean> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return source to pressed
}

@Composable
fun GlassStage(
    modifier: Modifier = Modifier,
    chrome: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize(), content = content)
        Box(Modifier.fillMaxSize(), content = chrome)
    }
}

/**
 * Bimodal Liquid Glass Atmospheric Canvas :
 * Gives Light Mode frosted translucent depth (cool ice sky + soft emerald ambient orbs)
 * matching Obsidian OLED Dark Mode depth.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDark.current
    val baseBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0A0D14),
                Color(0xFF0E131D),
                Color(0xFF090C11)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFE3ECF7),
                Color(0xFFF0F5FB),
                Color(0xFFE1F1EC)
            )
        )
    }

    val topGlow = Brush.radialGradient(
        colors = if (dark) {
            listOf(Color(0x24438DFF), Color.Transparent)
        } else {
            listOf(Color(0x3838BDF8), Color.Transparent)
        },
        center = Offset(820f, 180f),
        radius = 850f
    )

    val bottomGlow = Brush.radialGradient(
        colors = if (dark) {
            listOf(Color(0x1E10B981), Color.Transparent)
        } else {
            listOf(Color(0x2E10B981), Color.Transparent)
        },
        center = Offset(180f, 1500f),
        radius = 900f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
            .background(topGlow)
            .background(bottomGlow),
        content = content
    )
}

enum class GlassStyle { Regular, Clear, Menu, Card, Accent }

/**
 * Frosted Liquid Glassmorphism (Identique à CodeStation) :
 * - Translucent frosted gradient surface in both Light & Dark Modes
 * - Crisp top specular highlight rim + subtle bottom edge
 */
@Composable
fun Modifier.glass(
    shape: Shape = Capsule(),
    style: GlassStyle = GlassStyle.Regular,
    tint: Color = Color.Unspecified,
    pressed: Boolean = false
): Modifier {
    val dark = LocalDark.current
    val base = if (tint.isSpecified) {
        tint
    } else {
        when (style) {
            GlassStyle.Regular -> if (dark) Color(0x45171B24) else Color(0xB8FFFFFF)
            GlassStyle.Clear -> if (dark) Color(0x2812151C) else Color(0x88FFFFFF)
            GlassStyle.Menu -> if (dark) Color(0xE6171B24) else Color(0xE8FFFFFF)
            GlassStyle.Card -> if (dark) Color(0x4D1A202C) else Color(0xB3FFFFFF)
            GlassStyle.Accent -> if (dark) Color(0x33438DFF) else Color(0x221D4ED8)
        }
    }

    val rimBrush = if (dark) {
        if (style == GlassStyle.Accent) {
            Brush.verticalGradient(listOf(Color(0xFF6BA6FF), Color(0x80438DFF), Color(0x33438DFF)))
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (style == GlassStyle.Menu) 0.55f else 0.72f),
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.98f),
                Color(0xFF94A3B8).copy(alpha = 0.28f),
                Color(0xFF64748B).copy(alpha = 0.22f)
            )
        )
    }

    val bgBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (pressed) 0.18f else 0.10f),
                base.copy(alpha = if (pressed) 0.85f else 0.58f),
                base.copy(alpha = if (pressed) 0.95f else 0.74f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (pressed) 0.96f else 0.86f),
                base.copy(alpha = if (pressed) 0.82f else 0.72f),
                Color(0xFFF1F5F9).copy(alpha = if (pressed) 0.90f else 0.78f)
            )
        )
    }

    return this
        .background(bgBrush, shape)
        .border(BorderStroke(1.dp, rimBrush), shape)
        .clip(shape)
}

@Composable
fun Modifier.glassCard(
    shape: Shape = RoundedRectangle(18.dp),
    pressed: Boolean = false
): Modifier = this.glass(shape = shape, style = GlassStyle.Card, pressed = pressed)

/**
 * Uniform-height primary action button (42dp) with crisp guaranteed high contrast & zero text overflow.
 */
@Composable
fun GlassSolidButton(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = Palette.emeraldSolidBg,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 42.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = Capsule(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.42f),
            disabledContentColor = contentColor.copy(alpha = 0.85f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = modifier.height(height)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = Type.label.bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Uniform-height soft frosted button (42dp) with crisp readable colored ink on subtle glass fill (NEVER white-on-light).
 */
@Composable
fun GlassSoftButton(
    text: String,
    icon: ImageVector? = null,
    accentColor: Color = Palette.electricBlue,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 42.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val fillAlpha = if (dark) 0.22f else 0.14f
    val borderAlpha = if (dark) 0.48f else 0.38f

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = Capsule(),
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = borderAlpha)),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor.copy(alpha = fillAlpha),
            contentColor = accentColor,
            disabledContainerColor = accentColor.copy(alpha = 0.08f),
            disabledContentColor = accentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = modifier.height(height)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = Type.label.bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Uniform frosted filter/preset pill chip avoiding layered glass-over-fill contrast bugs.
 */
@Composable
fun GlassFilterChip(
    text: String,
    selected: Boolean,
    selectedColor: Color = Palette.emeraldSolidBg,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val bgColor = when {
        selected -> selectedColor
        dark -> Color(0x331A202C)
        else -> Color(0xB8FFFFFF)
    }
    val borderColor = when {
        selected -> selectedColor
        dark -> Color(0x38FFFFFF)
        else -> Color(0x300F172A)
    }
    val textColor = if (selected) Color.White else Palette.text

    Box(
        modifier = modifier
            .height(34.dp)
            .background(bgColor, Capsule())
            .border(0.75.dp, borderColor, Capsule())
            .clip(Capsule())
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = Type.label.bold,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun glassFieldColors(): TextFieldColors {
    val dark = LocalDark.current
    val container = if (dark) Color(0x33000000) else Color(0x99FFFFFF)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = Palette.text,
        unfocusedTextColor = Palette.text,
        focusedLabelColor = Palette.electricBlue,
        unfocusedLabelColor = Palette.secondary,
        focusedPlaceholderColor = Palette.tertiary,
        unfocusedPlaceholderColor = Palette.tertiary,
        focusedBorderColor = Palette.electricBlue,
        unfocusedBorderColor = Palette.glassBorder,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        cursorColor = Palette.electricBlue
    )
}
