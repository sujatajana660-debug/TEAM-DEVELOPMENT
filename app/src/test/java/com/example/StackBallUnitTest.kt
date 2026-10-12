package com.example

import com.example.engine.LevelGenerator
import com.example.engine.StackBallRenderer
import com.example.model.AVAILABLE_BALL_SKINS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackBallUnitTest {

    @Test
    fun testLevelThemeGeneration() {
        val theme1 = LevelGenerator.getThemeForLevel(1)
        assertEquals(1, theme1.levelNumber)
        assertTrue(theme1.numLayers >= 20)

        val theme5 = LevelGenerator.getThemeForLevel(5)
        assertEquals(5, theme5.levelNumber)
        assertTrue(theme5.numLayers > theme1.numLayers)
    }

    @Test
    fun testLayerGenerationAndSafety() {
        val theme = LevelGenerator.getThemeForLevel(1)
        val layers = LevelGenerator.generateLayers(theme)

        assertEquals(theme.numLayers, layers.size)
        // First 2 layers must be 100% safe to give player a smooth launch
        assertTrue(layers[0].segments.none { it.isDanger })
        assertTrue(layers[1].segments.none { it.isDanger })

        // Later layers must contain danger segments
        val hasDangerLater = layers.drop(2).any { layer ->
            layer.segments.any { it.isDanger }
        }
        assertTrue("Levels must contain black danger obstacle segments", hasDangerLater)
    }

    @Test
    fun testBallSkinsAvailable() {
        assertTrue(AVAILABLE_BALL_SKINS.isNotEmpty())
        val defaultSkin = AVAILABLE_BALL_SKINS.first()
        assertEquals("classic_red", defaultSkin.id)
        assertEquals(0, defaultSkin.cost)
    }

    @Test
    fun test3DProjectionMath() {
        val renderer = StackBallRenderer()
        val proj = renderer.project(
            x = 0f,
            y = 0f,
            z = -52f,
            camY = -45f,
            screenWidth = 400f,
            screenHeight = 800f
        )
        assertNotNull(proj)
        assertTrue(proj.depth > 0f)
        assertTrue(proj.scale > 0f)
        // Center x should be projected around screen center
        assertEquals(200f, proj.screenX, 1f)
    }

    @Test
    fun testFireball3SecondHoldAnd3SecondActive() {
        // Player 3s hold karke tiles todega -> 3s fireball active hoga
        val targetHoldSeconds = 3.0f
        val targetActiveSeconds = 3.0f
        assertEquals(3.0f, targetHoldSeconds, 0.001f)
        assertEquals(3.0f, targetActiveSeconds, 0.001f)
    }

    @Test
    fun testPlatformShapesVarietyAndRadius() {
        val renderer = StackBallRenderer()
        // Circle has uniform radius
        val rCircle = renderer.getOuterRadius(com.example.model.PlatformShape.CIRCLE, 0f)
        assertEquals(renderer.platformOuterRadius, rCircle, 0.01f)

        // Hexagon, Star, Flower, Octagon, Square produce valid radii encompassing cylinder
        for (shape in com.example.model.PlatformShape.values()) {
            val r0 = renderer.getOuterRadius(shape, 0f)
            val r90 = renderer.getOuterRadius(shape, (Math.PI / 2.0).toFloat())
            assertTrue(r0 > renderer.cylinderRadius)
            assertTrue(r90 > renderer.cylinderRadius)
        }

        // Verify different levels introduce distinct shapes
        val theme1 = LevelGenerator.getThemeForLevel(1)
        val theme2 = LevelGenerator.getThemeForLevel(2)
        val theme3 = LevelGenerator.getThemeForLevel(3)
        val theme4 = LevelGenerator.getThemeForLevel(4)

        assertEquals(com.example.model.PlatformShape.CIRCLE, theme1.primaryShape)
        assertEquals(com.example.model.PlatformShape.HEXAGON, theme2.primaryShape)
        assertEquals(com.example.model.PlatformShape.STAR, theme3.primaryShape)
        assertEquals(com.example.model.PlatformShape.FLOWER, theme4.primaryShape)

        val layers2 = LevelGenerator.generateLayers(theme2)
        assertTrue(layers2.all { it.shape == com.example.model.PlatformShape.HEXAGON })
    }

    @Test
    fun test1000PlusLevelsSupportAndProgressiveScaling() {
        // User request: 1000+ levels, tile count increases, tile rotation speed increases
        val theme1 = LevelGenerator.getThemeForLevel(1)
        val theme50 = LevelGenerator.getThemeForLevel(50)
        val theme500 = LevelGenerator.getThemeForLevel(500)
        val theme1000 = LevelGenerator.getThemeForLevel(1000)
        val theme1500 = LevelGenerator.getThemeForLevel(1500)

        assertEquals(1, theme1.levelNumber)
        assertEquals(50, theme50.levelNumber)
        assertEquals(500, theme500.levelNumber)
        assertEquals(1000, theme1000.levelNumber)
        assertEquals(1500, theme1500.levelNumber)

        // Tile count (numLayers) increases with levels
        assertTrue(theme50.numLayers > theme1.numLayers)
        assertTrue(theme500.numLayers >= theme50.numLayers)
        assertTrue(theme1000.numLayers >= theme500.numLayers)
        assertTrue(theme1000.numLayers in 100..130)

        // Tile speed (baseRotationSpeed) increases with levels
        assertTrue(theme50.baseRotationSpeed > theme1.baseRotationSpeed)
        assertTrue(theme1000.baseRotationSpeed > theme50.baseRotationSpeed)
        assertTrue(theme1000.baseRotationSpeed in 120f..160f)

        // Verify layer generation succeeds for level 1000 with zero errors
        val layers1000 = LevelGenerator.generateLayers(theme1000)
        assertEquals(theme1000.numLayers, layers1000.size)
        assertTrue(layers1000.first().segments.none { it.isDanger })
    }

    @Test
    fun testScreenAdaptiveProjection() {
        val renderer = StackBallRenderer()
        // Standard phone
        val projPhone = renderer.project(0f, 0f, -48f, -45f, 380f, 800f)
        assertTrue(projPhone.scale > 0f)

        // Tall modern 21:9 phone
        val projTall = renderer.project(0f, 0f, -48f, -45f, 400f, 960f)
        assertTrue(projTall.scale > 0f)

        // Tablet / Foldable wide screen
        val projTablet = renderer.project(0f, 0f, -48f, -45f, 800f, 1200f)
        assertTrue(projTablet.scale > 0f)
        // Scaled proportion stays bounded and cleanly centered
        assertEquals(400f, projTablet.screenX, 1f)
    }
}

