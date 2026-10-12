package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayState {
    WAITING_TO_START,
    PLAYING,
    GAME_OVER,
    LEVEL_COMPLETED
}

data class BallSkin(
    val id: String,
    val name: String,
    val cost: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val highlightColor: Color,
    val iconDescription: String
)

val AVAILABLE_BALL_SKINS = listOf(
    BallSkin(
        id = "classic_red",
        name = "Classic Red",
        cost = 0,
        primaryColor = Color(0xFFFF2A2A),
        secondaryColor = Color(0xFFB71C1C),
        highlightColor = Color(0xFFFF8A80),
        iconDescription = "Classic fiery red arcade sphere"
    ),
    BallSkin(
        id = "electric_blue",
        name = "Electric Blue",
        cost = 80,
        primaryColor = Color(0xFF00B0FF),
        secondaryColor = Color(0xFF0077C2),
        highlightColor = Color(0xFF80D8FF),
        iconDescription = "Vibrant neon cyan blue"
    ),
    BallSkin(
        id = "lemon_yellow",
        name = "Lemon Neon",
        cost = 140,
        primaryColor = Color(0xFFFFD600),
        secondaryColor = Color(0xFFFFAB00),
        highlightColor = Color(0xFFFFFF8D),
        iconDescription = "High-voltage sunshine yellow"
    ),
    BallSkin(
        id = "lime_toxic",
        name = "Radioactive Lime",
        cost = 200,
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF00A152),
        highlightColor = Color(0xFFB9F6CA),
        iconDescription = "Glowing emerald lime"
    ),
    BallSkin(
        id = "magma_flame",
        name = "Magma Core",
        cost = 320,
        primaryColor = Color(0xFFFF5722),
        secondaryColor = Color(0xFFD84315),
        highlightColor = Color(0xFFFFCCBC),
        iconDescription = "Volcanic molten core"
    ),
    BallSkin(
        id = "basketball",
        name = "Basketball",
        cost = 450,
        primaryColor = Color(0xFFFF6F00),
        secondaryColor = Color(0xFF263238),
        highlightColor = Color(0xFFFFD54F),
        iconDescription = "Street slam basketball"
    ),
    BallSkin(
        id = "eight_ball",
        name = "Eight Ball",
        cost = 580,
        primaryColor = Color(0xFF212121),
        secondaryColor = Color(0xFF000000),
        highlightColor = Color(0xFFECEFF1),
        iconDescription = "Billiard stealth 8-ball"
    ),
    BallSkin(
        id = "amethyst",
        name = "Cosmic Amethyst",
        cost = 750,
        primaryColor = Color(0xFF9C27B0),
        secondaryColor = Color(0xFF6A1B9A),
        highlightColor = Color(0xFFE1BEE7),
        iconDescription = "Deep galactic crystal"
    ),
    BallSkin(
        id = "cyber_gold",
        name = "Royal 24K Gold",
        cost = 1000,
        primaryColor = Color(0xFFFFC107),
        secondaryColor = Color(0xFFFF8F00),
        highlightColor = Color(0xFFFFF8E1),
        iconDescription = "Pure shiny 24 karat gold"
    )
)

enum class PlatformShape(val displayName: String, val badgeText: String) {
    CIRCLE("Circle Ring", "CIRCLE"),
    HEXAGON("Hexagon Prism", "HEXAGON"),
    OCTAGON("Octagon Prism", "OCTAGON"),
    FLOWER("Flower Petals", "PETAL FLOWER"),
    STAR("Star Cogwheel", "STAR COG"),
    SQUARE("Square Diamond", "SQUARE")
}

data class PlatformSegment(
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
    val isDanger: Boolean
)

data class PaintSplat(
    val localAngleDeg: Float,
    val radiusRatio: Float, // distance from center (0.5 .. 0.95)
    val size: Float,
    val color: Color
)

class PlatformLayer(
    val layerIndex: Int,
    val y: Float,
    val segments: List<PlatformSegment>,
    var isBroken: Boolean = false,
    var rotationAngle: Float = 0f,
    val rotSpeed: Float = 60f, // degrees per second
    val splats: MutableList<PaintSplat> = mutableListOf(),
    val shape: PlatformShape = PlatformShape.CIRCLE
)

data class DebrisFragment(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var rotX: Float = 0f,
    var rotY: Float = 0f,
    var rotZ: Float = 0f,
    var vRotX: Float = 0f,
    var vRotY: Float = 0f,
    var vRotZ: Float = 0f,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float = 16f,
    val color: Color,
    val highlightColor: Color = color,
    var alpha: Float = 1f,
    var life: Float = 1.4f,
    val isTileChunk: Boolean = false,
    val shape: PlatformShape = PlatformShape.CIRCLE
)

data class BreakShockwave(
    val y: Float,
    var radius: Float = 14f,
    val maxRadius: Float = 95f,
    val color: Color,
    var alpha: Float = 0.95f
)

data class AmbientParticle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var alpha: Float,
    var phase: Float
)

data class LevelTheme(
    val levelNumber: Int,
    val bgTopColor: Color,
    val bgBottomColor: Color,
    val platformColor: Color,
    val platformHighlightColor: Color,
    val dangerColor: Color = Color(0xFF1E2024),
    val numLayers: Int,
    val baseRotationSpeed: Float,
    val rotatesClockwise: Boolean,
    val oscillates: Boolean,
    val primaryShape: PlatformShape = PlatformShape.CIRCLE
)
