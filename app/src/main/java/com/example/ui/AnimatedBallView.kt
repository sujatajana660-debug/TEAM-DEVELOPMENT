package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.BallSkin
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-quality 3D animated ball showcase with dynamic bounce, squash-and-stretch,
 * rotating specular gleam, and realistic platform shadow.
 */
@Composable
fun AnimatedBallView(
    skin: BallSkin,
    size: Dp = 120.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ball_anim")

    // Bounce vertical translation (0f to 1f)
    val bounceProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Continuous specular rotation angle
    val rotAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w * 0.5f
            val baseFloorY = h * 0.82f
            val ballRadius = w * 0.32f

            // Bounce physics: parabolic height
            val maxBounceHeight = h * 0.28f
            val currentBounceY = baseFloorY - ballRadius - (1f - bounceProgress) * maxBounceHeight

            // Squash & stretch: when hitting floor (bounceProgress near 1f)
            val isNearFloor = bounceProgress > 0.85f
            val squashFactor = if (isNearFloor) {
                1f - (bounceProgress - 0.85f) / 0.15f * 0.22f
            } else {
                1f + (1f - bounceProgress) * 0.08f // slight stretch in air
            }
            val stretchX = if (isNearFloor) 1f + (bounceProgress - 0.85f) / 0.15f * 0.22f else 1f

            // 1. Draw Pedestal / Floor Shadow underneath
            val shadowWidth = ballRadius * (1.6f + bounceProgress * 0.4f)
            val shadowHeight = ballRadius * (0.35f + bounceProgress * 0.15f)
            val shadowAlpha = (0.2f + bounceProgress * 0.35f).coerceIn(0.1f, 0.6f)

            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        Color.Black.copy(alpha = shadowAlpha),
                        Color.Black.copy(alpha = shadowAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, baseFloorY),
                    radius = shadowWidth * 0.5f
                ),
                topLeft = Offset(centerX - shadowWidth * 0.5f, baseFloorY - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight)
            )

            // 2. Draw 3D Sphere with radial gradient lighting
            val radAngleRad = Math.toRadians(rotAngle.toDouble())
            val lightOffsetX = (ballRadius * 0.32f * cos(radAngleRad)).toFloat()
            val lightOffsetY = (ballRadius * 0.32f * sin(radAngleRad)).toFloat()

            val ballBrush = Brush.radialGradient(
                listOf(
                    skin.highlightColor,
                    skin.primaryColor,
                    skin.secondaryColor,
                    Color(0xFF141414)
                ),
                center = Offset(
                    centerX - ballRadius * 0.25f + lightOffsetX * 0.4f,
                    currentBounceY - ballRadius * 0.28f + lightOffsetY * 0.4f
                ),
                radius = ballRadius * 1.25f
            )

            // Draw squashed/stretched ball
            drawOval(
                brush = ballBrush,
                topLeft = Offset(
                    centerX - ballRadius * stretchX,
                    currentBounceY - ballRadius * squashFactor
                ),
                size = Size(ballRadius * 2f * stretchX, ballRadius * 2f * squashFactor)
            )

            // 3. Specular gloss glint
            val glintRadius = ballRadius * 0.22f
            val glintX = centerX - ballRadius * 0.32f * stretchX
            val glintY = currentBounceY - ballRadius * 0.36f * squashFactor

            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = glintRadius,
                center = Offset(glintX, glintY)
            )

            // Secondary softer rim reflection
            drawCircle(
                color = skin.highlightColor.copy(alpha = 0.45f),
                radius = glintRadius * 0.6f,
                center = Offset(glintX + glintRadius * 0.3f, glintY + glintRadius * 0.3f)
            )
        }
    }
}
