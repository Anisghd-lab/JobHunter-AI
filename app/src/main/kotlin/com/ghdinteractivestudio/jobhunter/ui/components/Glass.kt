package com.ghdinteractivestudio.jobhunter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Motion
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette

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

enum class Edge { Top, Bottom }

@Composable
fun ScrollEdge(edge: Edge, height: Dp, modifier: Modifier = Modifier) {
    val ground = Palette.background
    val top = edge == Edge.Top
    val wash = if (top) {
        arrayOf(
            0.0f to ground,
            0.60f to ground,
            0.85f to ground.copy(alpha = 0.7f),
            1.0f to ground.copy(alpha = 0f)
        )
    } else {
        arrayOf(
            0.0f to ground.copy(alpha = 0f),
            0.15f to ground.copy(alpha = 0.7f),
            0.40f to ground,
            1.0f to ground
        )
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(colorStops = wash))
    )
}

enum class GlassStyle { Regular, Clear, Menu, Card, Accent }

/**
 * Frosted Liquid Glassmorphism (Identique à CodeStation) :
 * - Specular gradient border (bright rim on top, softer ambient on bottom)
 * - Frosted translucent fill with subtle liquid reflection
 * - Inner stabilized plate compatibility for maximum legibility
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
            GlassStyle.Regular -> if (dark) Color(0x28171B24) else Color(0xD9FFFFFF)
            GlassStyle.Clear -> if (dark) Color(0x1812151C) else Color(0xB3FFFFFF)
            GlassStyle.Menu -> if (dark) Color(0xE6171B24) else Color(0xF2FFFFFF)
            GlassStyle.Card -> if (dark) Color(0x401A202C) else Color(0xEEFFFFFF)
            GlassStyle.Accent -> if (dark) Color(0x33438DFF) else Color(0x26438DFF)
        }
    }

    val rimBrush = if (dark) {
        if (style == GlassStyle.Accent) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF6BA6FF),
                    Color(0x80438DFF),
                    Color(0x33438DFF)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (style == GlassStyle.Menu) 0.55f else 0.75f),
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.Black.copy(alpha = 0.06f),
                Color.Black.copy(alpha = 0.14f)
            )
        )
    }

    val bgBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (pressed) 0.18f else 0.10f),
                base.copy(alpha = if (pressed) 0.85f else 0.55f),
                base.copy(alpha = if (pressed) 0.95f else 0.70f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                base.copy(alpha = 0.80f),
                base.copy(alpha = 0.90f)
            )
        )
    }

    return this
        .background(bgBrush, shape)
        .border(BorderStroke(1.dp, rimBrush), shape)
        .clip(shape)
}

/**
 * Liquid Glass Card helper with inner stabilized plate.
 */
@Composable
fun Modifier.glassCard(
    shape: Shape = com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle(18.dp),
    pressed: Boolean = false
): Modifier = this.glass(shape = shape, style = GlassStyle.Card, pressed = pressed)
