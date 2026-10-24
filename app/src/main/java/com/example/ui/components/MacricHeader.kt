package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameModel
import com.example.ui.theme.*
import com.example.viewmodel.MacricTab

@Composable
fun MacricHeader(
    currentTab: MacricTab,
    connectedGame: GameModel?,
    isOverlayActive: Boolean,
    onTabSelected: (MacricTab) -> Unit,
    onToggleOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MacricDarkSurface)
            .border(width = 0.5.dp, color = MacricBorder)
    ) {
        // Top status row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onTabSelected(MacricTab.HOME) }
                    .testTag("macric_logo_brand")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MacricViolet, MacricVioletDark))
                        )
                        .border(1.dp, MacricVioletLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "MACRIC Bolt",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "MACRIC",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MacricTextPrimary,
                    letterSpacing = 1.sp
                )
            }

            // Center / Right status: Connected game & Overlay pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectedGame != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MacricDarkCardElevated)
                            .border(1.dp, MacricGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(MacricTab.GAMES) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MacricGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = connectedGame.name,
                            color = MacricTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MacricDarkCard)
                            .border(1.dp, MacricBorder, RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(MacricTab.GAMES) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MacricTextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "No Game Linked",
                            color = MacricTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Overlay HUD Action Toggle
                Button(
                    onClick = onToggleOverlay,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("toggle_overlay_hud_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayActive) MacricGreen else MacricVioletDark
                    )
                ) {
                    Icon(
                        imageVector = if (isOverlayActive) Icons.Default.Layers else Icons.Default.LayersClear,
                        contentDescription = "Overlay Status",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOverlayActive) "HUD ON" else "HUD OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Horizontal navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MacricTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(
                                listOf(MacricViolet, MacricVioletDark)
                            ) else Brush.horizontalGradient(
                                listOf(MacricDarkCard, MacricDarkCard)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MacricVioletLight else MacricBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) Color.White else MacricTextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
