package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StackBallHUD(
    level: Int,
    activeLayerIndex: Int,
    totalLayers: Int,
    score: Int,
    fireMeter: Float,
    isFireball: Boolean,
    shape: com.example.model.PlatformShape = com.example.model.PlatformShape.CIRCLE,
    onHomeClick: (() -> Unit)? = null
) {
    val progress = if (totalLayers > 0) {
        (activeLayerIndex.toFloat() / totalLayers.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "level_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
        // Optional Home / Menu button on the left
        if (onHomeClick != null) {
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(38.dp)
                    .background(Color(0x66000000), CircleShape)
                    .testTag("hud_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Main Menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center HUD Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level Progress Track
            Row(
                modifier = Modifier
                    .width(260.dp)
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Circle: Current Level
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF2979FF), CircleShape)
                        .testTag("current_level_pill")
                ) {
                    Text(
                        text = "$level",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                // Middle Progress Bar Track
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x55000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2979FF), Color(0xFF00E5FF))
                                )
                            )
                    )
                }

                // Right Circle: Next Level
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color(0xFF00B0FF), CircleShape)
                        .testTag("next_level_pill")
                ) {
                    Text(
                        text = "${level + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Shape Badge
            Text(
                text = shape.displayName.uppercase(),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            // Score Display
            Text(
                text = "$score",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("score_counter")
            )

            // Fireball Mode / Fury Charge Meter (when charging or active)
            if (fireMeter > 0.05f || isFireball) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("fire_meter_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Fire Mode",
                        tint = if (isFireball) Color(0xFFFF3D00) else Color(0xFFFF9100),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x44FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fireMeter)
                                .height(6.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFEA00), Color(0xFFFF3D00))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFireball) "FIREBALL (3s)!" else "HOLD FOR FIRE",
                        color = if (isFireball) Color(0xFFFF3D00) else Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
