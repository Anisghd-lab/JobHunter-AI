package com.ghdinteractivestudio.jobhunter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.jobhunter.domain.model.DocumentFormatRequirement
import com.ghdinteractivestudio.jobhunter.domain.model.JobOffer
import com.ghdinteractivestudio.jobhunter.ui.components.GlassStyle
import com.ghdinteractivestudio.jobhunter.ui.components.glass
import com.ghdinteractivestudio.jobhunter.ui.components.glassCard
import com.ghdinteractivestudio.jobhunter.ui.theme.Capsule
import com.ghdinteractivestudio.jobhunter.ui.theme.LocalDark
import com.ghdinteractivestudio.jobhunter.ui.theme.Palette
import com.ghdinteractivestudio.jobhunter.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.jobhunter.ui.theme.Type
import com.ghdinteractivestudio.jobhunter.ui.theme.bold
import com.ghdinteractivestudio.jobhunter.ui.theme.monospaced
import com.ghdinteractivestudio.jobhunter.ui.theme.semibold
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RadarScreen(
    offersFlow: StateFlow<List<JobOffer>>,
    onApplyClick: (JobOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val offers by offersFlow.collectAsState()
    val dark = LocalDark.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Palette.emeraldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "RADAR D'OPPORTUNITÉS EN DIRECT",
                        style = Type.mono.bold,
                        color = Palette.emeraldAccent
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "L'IA surveille en continu le web mondial, analyse les exigences et détecte les formats requis.",
                    style = Type.bodySmall,
                    color = Palette.secondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Offer list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(offers, key = { it.id }) { offer ->
                JobOfferCard(offer = offer, onApplyClick = { onApplyClick(offer) })
            }
            item {
                Spacer(Modifier.height(90.dp)) // Clearance for bottom GlassTabBar
            }
        }
    }
}

@Composable
fun JobOfferCard(
    offer: JobOffer,
    onApplyClick: () -> Unit
) {
    val dark = LocalDark.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform tag
                Box(
                    modifier = Modifier
                        .glass(Capsule(), style = GlassStyle.Clear)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = offer.sourcePlatform,
                        style = Type.label.monospaced,
                        color = Palette.tertiary
                    )
                }

                // Format Requirement Badge
                val formatText = when (offer.requiredFormat) {
                    DocumentFormatRequirement.PDF -> "Format Requis : PDF"
                    DocumentFormatRequirement.DOCX -> "Format Requis : Word .docx"
                    DocumentFormatRequirement.ANY -> "Format : PDF / Word"
                }
                val formatColor = if (offer.requiredFormat == DocumentFormatRequirement.DOCX) Palette.electricBlue else Palette.emeraldAccent

                Box(
                    modifier = Modifier
                        .background(formatColor.copy(alpha = 0.15f), Capsule())
                        .border(0.5.dp, formatColor.copy(alpha = 0.4f), Capsule())
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatText,
                        style = Type.label.bold,
                        color = formatColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title & Company
            Text(
                text = offer.title,
                style = Type.titleMedium.bold,
                color = Palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Palette.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = offer.company,
                    style = Type.bodySmall.semibold,
                    color = Palette.secondary
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Palette.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = offer.location,
                    style = Type.bodySmall,
                    color = Palette.secondary
                )
            }

            Spacer(Modifier.height(10.dp))

            // Inner Stabilized Plate for description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (dark) Color(0x26000000) else Color(0x10000000),
                        RoundedRectangle(10.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = offer.description,
                    style = Type.bodySmall,
                    color = Palette.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match score
                Text(
                    text = if (offer.matchScore > 0) "Score Match : ${offer.matchScore}%" else "Score calculé à l'envoi",
                    style = Type.mono.bold,
                    color = if (offer.matchScore >= 80) Palette.emeraldAccent else Palette.attention
                )

                Button(
                    onClick = onApplyClick,
                    shape = Capsule(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.emeraldAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Postuler", style = Type.bodySmall.bold)
                }
            }
        }
    }
}
