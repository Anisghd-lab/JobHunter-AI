package com.ghdinteractivestudio.jobhunter.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Motion
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.Type

data class JobTabItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconSize: Dp = 24.dp
)

val MainJobTabs = listOf(
    JobTabItem("radar", "Radar", Icons.Default.Radar),
    JobTabItem("pipeline", "Suivi", Icons.Default.Assessment),
    JobTabItem("cv", "Mon CV", Icons.Default.Description),
    JobTabItem("agent", "Agent 24/7", Icons.Default.Settings)
)

private val TAB_WIDTH = 84.dp
private val TAB_HEIGHT = 54.dp

/**
 * Exact CodeStation Liquid Glass Navigation Bar :
 * - Translucent floating capsule with specular rim border
 * - Smooth animated pill translating behind the selected tab
 * - Dynamic color transitions between Dark Obsidian Mode and Light Mode
 */
@Composable
fun GlassTabBar(
    selectedRoute: String,
    tabs: List<JobTabItem> = MainJobTabs,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val index = tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val pillOffset by animateDpAsState(
        targetValue = TAB_WIDTH * index,
        animationSpec = Motion.layout(),
        label = "Tab selection pill"
    )
    val dark = LocalDark.current

    Box(
        modifier = modifier
            .glass(Capsule(), style = GlassStyle.Regular)
            .padding(4.dp)
    ) {
        val pillShape = Capsule()
        val pillBg = if (dark) Palette.emeraldAccent.copy(alpha = 0.18f) else Palette.emeraldAccent.copy(alpha = 0.14f)
        val pillBorder = if (dark) Palette.emeraldAccent.copy(alpha = 0.38f) else Palette.emeraldAccent.copy(alpha = 0.45f)

        // Animated indicator pill
        Box(
            Modifier
                .offset(x = pillOffset)
                .size(TAB_WIDTH, TAB_HEIGHT)
                .background(pillBg, pillShape)
                .border(0.75.dp, pillBorder, pillShape)
                .clip(pillShape)
        )

        Row {
            for (tab in tabs) {
                val isSelected = tab.route == selectedRoute
                val itemTint = if (isSelected) Palette.emeraldAccent else Palette.secondary

                Column(
                    modifier = Modifier
                        .size(TAB_WIDTH, TAB_HEIGHT)
                        .clip(Capsule())
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab.route) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = itemTint,
                        modifier = Modifier.size(tab.iconSize)
                    )
                    Text(
                        text = tab.title,
                        style = Type.system(10f),
                        color = itemTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
