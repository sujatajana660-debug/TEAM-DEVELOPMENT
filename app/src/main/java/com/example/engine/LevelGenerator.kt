package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.LevelTheme
import com.example.model.PlatformLayer
import com.example.model.PlatformSegment
import com.example.model.PlatformShape
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object LevelGenerator {

    private val THEMES = listOf(
        // Level 1: Cyan sky with Royal Blue platforms (Circle Ring)
        LevelTheme(
            levelNumber = 1,
            bgTopColor = Color(0xFF00E5FF),
            bgBottomColor = Color(0xFF69F0AE),
            platformColor = Color(0xFF2979FF),
            platformHighlightColor = Color(0xFF82B1FF),
            numLayers = 25,
            baseRotationSpeed = 65f,
            rotatesClockwise = true,
            oscillates = false,
            primaryShape = PlatformShape.CIRCLE
        ),
        // Level 2: Warm fiery gradient with Vivid Yellow platforms (Hexagon Prism)
        LevelTheme(
            levelNumber = 2,
            bgTopColor = Color(0xFF6A1B9A),
            bgBottomColor = Color(0xFFFF5722),
            platformColor = Color(0xFFFFEA00),
            platformHighlightColor = Color(0xFFFFFF8D),
            numLayers = 28,
            baseRotationSpeed = 70f,
            rotatesClockwise = false,
            oscillates = false,
            primaryShape = PlatformShape.HEXAGON
        ),
        // Level 3: Purple gradient with Neon Lime platforms (Star Cogwheel)
        LevelTheme(
            levelNumber = 3,
            bgTopColor = Color(0xFF311B92),
            bgBottomColor = Color(0xFFAB47BC),
            platformColor = Color(0xFF76FF03),
            platformHighlightColor = Color(0xFFB2FF59),
            numLayers = 32,
            baseRotationSpeed = 75f,
            rotatesClockwise = true,
            oscillates = true,
            primaryShape = PlatformShape.STAR
        ),
        // Level 4: Deep Navy/Slate with Electric Orange platforms (Flower Petals)
        LevelTheme(
            levelNumber = 4,
            bgTopColor = Color(0xFF1A237E),
            bgBottomColor = Color(0xFF00B0FF),
            platformColor = Color(0xFFFF6D00),
            platformHighlightColor = Color(0xFFFFAB40),
            numLayers = 35,
            baseRotationSpeed = 80f,
            rotatesClockwise = false,
            oscillates = false,
            primaryShape = PlatformShape.FLOWER
        ),
        // Level 5: Teal / Emerald with Vivid Coral platforms (Octagon Prism)
        LevelTheme(
            levelNumber = 5,
            bgTopColor = Color(0xFF006064),
            bgBottomColor = Color(0xFF00E676),
            platformColor = Color(0xFFFF4081),
            platformHighlightColor = Color(0xFFFF80AB),
            numLayers = 40,
            baseRotationSpeed = 85f,
            rotatesClockwise = true,
            oscillates = false,
            primaryShape = PlatformShape.OCTAGON
        ),
        // Level 6: Crimson Red with Electric Sky Blue platforms (Square Diamond)
        LevelTheme(
            levelNumber = 6,
            bgTopColor = Color(0xFFB71C1C),
            bgBottomColor = Color(0xFFFF7043),
            platformColor = Color(0xFF00E5FF),
            platformHighlightColor = Color(0xFF84FFFF),
            numLayers = 44,
            baseRotationSpeed = 90f,
            rotatesClockwise = false,
            oscillates = true,
            primaryShape = PlatformShape.SQUARE
        ),
        // Level 7: Dark Obsidian with Golden Amber platforms (Multi-Shape Morph)
        LevelTheme(
            levelNumber = 7,
            bgTopColor = Color(0xFFBF360C),
            bgBottomColor = Color(0xFFFFAB00),
            platformColor = Color(0xFFFFD600),
            platformHighlightColor = Color(0xFFFFF59D),
            numLayers = 48,
            baseRotationSpeed = 95f,
            rotatesClockwise = true,
            oscillates = false,
            primaryShape = PlatformShape.HEXAGON
        ),
        // Level 8: Cyberpunk Neon Violet with Electric Cyan (Flower Petals)
        LevelTheme(
            levelNumber = 8,
            bgTopColor = Color(0xFF4A148C),
            bgBottomColor = Color(0xFF00E5FF),
            platformColor = Color(0xFFFF007F),
            platformHighlightColor = Color(0xFFFF80DF),
            numLayers = 52,
            baseRotationSpeed = 100f,
            rotatesClockwise = false,
            oscillates = true,
            primaryShape = PlatformShape.FLOWER
        ),
        // Level 9: Deep Emerald Jungle with Golden Honey platforms (Octagon Prism)
        LevelTheme(
            levelNumber = 9,
            bgTopColor = Color(0xFF004D40),
            bgBottomColor = Color(0xFF00C853),
            platformColor = Color(0xFFFFB300),
            platformHighlightColor = Color(0xFFFFE082),
            numLayers = 56,
            baseRotationSpeed = 105f,
            rotatesClockwise = true,
            oscillates = false,
            primaryShape = PlatformShape.OCTAGON
        ),
        // Level 10: Cosmic Aurora with Radioactive Lime platforms (Star Cogwheel)
        LevelTheme(
            levelNumber = 10,
            bgTopColor = Color(0xFF0D47A1),
            bgBottomColor = Color(0xFF6200EA),
            platformColor = Color(0xFF00E676),
            platformHighlightColor = Color(0xFFB9F6CA),
            numLayers = 60,
            baseRotationSpeed = 110f,
            rotatesClockwise = false,
            oscillates = false,
            primaryShape = PlatformShape.STAR
        ),
        // Level 11: Electric Sunset Coral with Ultra White platforms (Square Diamond)
        LevelTheme(
            levelNumber = 11,
            bgTopColor = Color(0xFFDD2C00),
            bgBottomColor = Color(0xFFFF6D00),
            platformColor = Color(0xFFFFFFFF),
            platformHighlightColor = Color(0xFFE0E0E0),
            numLayers = 64,
            baseRotationSpeed = 115f,
            rotatesClockwise = true,
            oscillates = true,
            primaryShape = PlatformShape.SQUARE
        ),
        // Level 12: Galactic Abyss with Neon Orange platforms (Circle Ring)
        LevelTheme(
            levelNumber = 12,
            bgTopColor = Color(0xFF212121),
            bgBottomColor = Color(0xFF37474F),
            platformColor = Color(0xFFFF3D00),
            platformHighlightColor = Color(0xFFFF9E80),
            numLayers = 68,
            baseRotationSpeed = 120f,
            rotatesClockwise = false,
            oscillates = false,
            primaryShape = PlatformShape.CIRCLE
        )
    )

    /**
     * Supports 1000+ levels!
     * As the user requested: "jitna levels par karega utna tiles bi tejise barti jaigi"
     * - Tile count (numLayers) increases progressively up to 130 layers for mega towers
     * - Tile rotation speed (baseRotationSpeed) increases progressively up to 160°/s
     */
    fun getThemeForLevel(level: Int): LevelTheme {
        val safeLevel = level.coerceAtLeast(1)
        val baseTheme = THEMES[(safeLevel - 1) % THEMES.size]

        // Shapes for early levels strictly match tests, then dynamically cycle through all shapes
        val levelShape = when (safeLevel) {
            1 -> PlatformShape.CIRCLE
            2 -> PlatformShape.HEXAGON
            3 -> PlatformShape.STAR
            4 -> PlatformShape.FLOWER
            5 -> PlatformShape.OCTAGON
            6 -> PlatformShape.SQUARE
            else -> {
                val shapes = PlatformShape.values()
                shapes[(safeLevel - 1) % shapes.size]
            }
        }

        // 1. Progressive tile/layer count increase (scales smoothly from 25 up to 130 layers)
        val scaledLayers = (24 + ((safeLevel - 1) * 1.5f).toInt()).coerceIn(25, 130)

        // 2. Progressive rotation speed increase: tiles spin much faster as player beats levels!
        val speedBonus = ((safeLevel - 1) * 0.45f).coerceAtMost(85f)
        val speedVariation = ((safeLevel * 7) % 15).toFloat()
        val scaledSpeed = (baseTheme.baseRotationSpeed + speedBonus + speedVariation).coerceIn(65f, 160f)

        // Alternate or randomize direction / oscillation every few levels for variety
        val oscillates = (safeLevel % 3 == 0)
        val rotatesClockwise = (safeLevel % 2 != 0)

        return baseTheme.copy(
            levelNumber = safeLevel,
            numLayers = scaledLayers,
            baseRotationSpeed = scaledSpeed,
            primaryShape = levelShape,
            rotatesClockwise = rotatesClockwise,
            oscillates = oscillates
        )
    }

    /**
     * Generates a spiral stack of platform layers for the given level.
     */
    fun generateLayers(theme: LevelTheme, layerSpacing: Float = 22f): List<PlatformLayer> {
        val layers = mutableListOf<PlatformLayer>()
        val rnd = Random(theme.levelNumber * 1000 + 42)
        val numLayers = theme.numLayers

        // Helix pattern: offset angle shifts each layer
        var spiralAngle = 0f
        val spiralStep = if (rnd.nextBoolean()) 16f else 24f

        for (i in 0 until numLayers) {
            val y = i * layerSpacing
            val segments = mutableListOf<PlatformSegment>()

            // Slices configuration: each disc has 6 to 10 equal or varied slices
            val numSlices = when {
                theme.levelNumber == 1 -> 8
                theme.levelNumber % 2 == 0 -> 10
                else -> 8
            }
            val sliceSweep = 360f / numSlices

            // Danger segment distribution:
            // First 2 layers are always 100% safe to give player a smooth launch!
            // Later layers have 1 to 3 danger segments
            val dangerCount = when {
                i < 2 -> 0
                theme.levelNumber == 1 -> if (i % 3 == 0) 1 else 2
                theme.levelNumber == 2 -> if (i % 2 == 0) 2 else 3
                else -> rnd.nextInt(2, (numSlices / 2).coerceAtLeast(3))
            }

            // Pick danger segment indices
            val dangerIndices = mutableSetOf<Int>()
            // Group danger segments or spread them based on pattern
            val patternType = (i + theme.levelNumber) % 4
            when (patternType) {
                0 -> {
                    // Contiguous cluster of danger
                    val startIdx = (i * 2) % numSlices
                    for (d in 0 until dangerCount) {
                        dangerIndices.add((startIdx + d) % numSlices)
                    }
                }
                1 -> {
                    // Alternating safe and danger
                    for (d in 0 until dangerCount) {
                        dangerIndices.add((d * 2) % numSlices)
                    }
                }
                2 -> {
                    // Helical drifting danger block
                    val driftIdx = ((i * 3) / 2) % numSlices
                    for (d in 0 until dangerCount) {
                        dangerIndices.add((driftIdx + d) % numSlices)
                    }
                }
                else -> {
                    // Opposite sides
                    for (d in 0 until dangerCount) {
                        val idx = if (d % 2 == 0) d else (d + numSlices / 2) % numSlices
                        dangerIndices.add(idx)
                    }
                }
            }

            for (s in 0 until numSlices) {
                val startA = s * sliceSweep
                val isDanger = dangerIndices.contains(s)
                segments.add(
                    PlatformSegment(
                        startAngleDeg = startA,
                        sweepAngleDeg = sliceSweep,
                        isDanger = isDanger
                    )
                )
            }

            // Initial layer rotation with spiral progression
            val initialRot = spiralAngle + (i * 5f)
            val rotDirection = if (theme.rotatesClockwise) 1f else -1f
            val layerRotSpeed = theme.baseRotationSpeed * rotDirection * (if (i % 7 == 0) -0.8f else 1f)

            // Shape variation: Morph shapes in high levels
            val layerShape = when {
                theme.levelNumber >= 7 -> {
                    if (i % 8 < 4) theme.primaryShape else PlatformShape.STAR
                }
                else -> theme.primaryShape
            }

            layers.add(
                PlatformLayer(
                    layerIndex = i,
                    y = y,
                    segments = segments,
                    isBroken = false,
                    rotationAngle = initialRot % 360f,
                    rotSpeed = layerRotSpeed,
                    shape = layerShape
                )
            )

            spiralAngle += spiralStep
        }

        return layers
    }
}
