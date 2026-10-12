package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.model.AmbientParticle
import com.example.model.BallSkin
import com.example.model.BreakShockwave
import com.example.model.DebrisFragment
import com.example.model.LevelTheme
import com.example.model.PaintSplat
import com.example.model.PlatformLayer
import com.example.model.PlayState
import com.example.util.HapticManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class StackBallGameEngine(
    val prefs: GamePreferences,
    val soundManager: SoundManager,
    val hapticManager: HapticManager
) {
    val layerSpacing = 22f

    // Constants for 3s hold -> 3s fireball mechanic
    val holdDurationForFireball = 3.0f // Player holds for 3s
    val fireballActiveDuration = 3.0f  // Fireball remains active for 3s

    var playState: PlayState by mutableStateOf(PlayState.WAITING_TO_START)

    var currentLevel: Int by mutableIntStateOf(prefs.currentLevel)
        private set

    var score: Int by mutableIntStateOf(0)
        private set

    var coins: Int by mutableIntStateOf(prefs.coins)
        private set

    var highScore: Int by mutableIntStateOf(prefs.highScore)
        private set

    var activeLayerIndex: Int by mutableIntStateOf(0)
        private set

    var totalLayers: Int by mutableIntStateOf(25)
        private set

    var ballY: Float by mutableFloatStateOf(-20f)
        private set

    var camY: Float by mutableFloatStateOf(-65f)
        private set

    var isPressing: Boolean by mutableStateOf(false)

    var isFireball: Boolean by mutableStateOf(false)
        private set

    var fireMeter: Float by mutableFloatStateOf(0f)
        private set

    var fireballTimeRemaining: Float = 0f
        private set

    var continuousSmashTimer: Float = 0f
        private set

    private var comboStreak: Int = 1
    private var bouncePhase: Float = 0f
    private val bounceHeight: Float = 28f
    private val bounceSpeed: Float = 8.5f // rads/sec
    private var smashCooldown: Float = 0f
    private var hasDippedThisBounce: Boolean = false

    private fun normalizeAngle(deg: Float): Float {
        var a = deg % 360f
        if (a < 0f) a += 360f
        return a
    }

    var screenShake: Float = 0f
        private set

    var currentTheme: LevelTheme by mutableStateOf(LevelGenerator.getThemeForLevel(prefs.currentLevel))
        private set

    var layers: List<PlatformLayer> = emptyList()
        private set

    val debrisList = mutableListOf<DebrisFragment>()
    val shockwaves = mutableListOf<BreakShockwave>()
    val ambientParticles = mutableListOf<AmbientParticle>()

    init {
        // Initialize ambient background particles
        val rnd = Random(55)
        for (i in 0 until 35) {
            ambientParticles.add(
                AmbientParticle(
                    x = rnd.nextFloat(),
                    y = rnd.nextFloat(),
                    radius = rnd.nextFloat() * 6f + 2f,
                    speed = rnd.nextFloat() * 0.05f + 0.02f,
                    alpha = rnd.nextFloat() * 0.25f + 0.08f,
                    phase = rnd.nextFloat() * 2f * PI.toFloat()
                )
            )
        }
        startLevel(prefs.currentLevel)
    }

    fun startLevel(level: Int) {
        currentLevel = level
        prefs.currentLevel = level
        currentTheme = LevelGenerator.getThemeForLevel(level)
        totalLayers = currentTheme.numLayers
        layers = LevelGenerator.generateLayers(currentTheme, layerSpacing)

        activeLayerIndex = 0
        ballY = -bounceHeight
        camY = ballY - 45f
        bouncePhase = 0f
        isFireball = false
        fireMeter = 0f
        fireballTimeRemaining = 0f
        continuousSmashTimer = 0f
        comboStreak = 1
        debrisList.clear()
        shockwaves.clear()
        screenShake = 0f
        playState = PlayState.WAITING_TO_START
    }

    fun retryLevel() {
        startLevel(currentLevel)
    }

    fun nextLevel() {
        startLevel(currentLevel + 1)
    }

    fun addCoins(amount: Int) {
        coins += amount
        prefs.coins = coins
    }

    fun update(dt: Float, currentSkin: BallSkin) {
        val clampedDt = dt.coerceIn(0.001f, 0.06f)

        // 1. Update Ambient Floating Particles
        for (p in ambientParticles) {
            p.y -= p.speed * clampedDt
            if (p.y < -0.1f) {
                p.y = 1.1f
                p.x = Random.nextFloat()
            }
            p.phase += clampedDt * 1.5f
        }

        // 2. Screen Shake decay
        if (screenShake > 0f) {
            screenShake = (screenShake - clampedDt * 18f).coerceAtLeast(0f)
        }

        // 3. Update Platform Rotations
        for (layer in layers) {
            if (!layer.isBroken) {
                val speed = if (currentTheme.oscillates) {
                    layer.rotSpeed * sin(System.currentTimeMillis() / 800.0).toFloat()
                } else {
                    layer.rotSpeed
                }
                layer.rotationAngle = (layer.rotationAngle + speed * clampedDt) % 360f
                if (layer.rotationAngle < 0f) layer.rotationAngle += 360f
            }
        }

        // 4. Update Shattered Debris Physics
        val debrisIterator = debrisList.iterator()
        while (debrisIterator.hasNext()) {
            val d = debrisIterator.next()
            d.x += d.vx * clampedDt
            d.y += d.vy * clampedDt
            d.z += d.vz * clampedDt
            d.vy += 850f * clampedDt // Gravity
            d.rotX += d.vRotX * clampedDt
            d.rotY += d.vRotY * clampedDt
            d.rotZ += d.vRotZ * clampedDt
            d.life -= clampedDt * 0.95f
            if (d.life <= 0f) {
                debrisIterator.remove()
            }
        }

        // 4b. Update Break Shockwaves
        val shockIterator = shockwaves.iterator()
        while (shockIterator.hasNext()) {
            val sw = shockIterator.next()
            sw.radius += 260f * clampedDt
            sw.alpha -= 2.6f * clampedDt
            if (sw.alpha <= 0f || sw.radius >= sw.maxRadius) {
                shockIterator.remove()
            }
        }

        if (playState == PlayState.GAME_OVER || playState == PlayState.LEVEL_COMPLETED) {
            // Smooth camera follow even when level ended
            camY += (ballY - 45f - camY) * (clampedDt * 5f)
            return
        }

        // 5. Fireball Duration & Charge Meter (Player 3s hold -> 3s fireball active)
        if (isFireball) {
            fireballTimeRemaining -= clampedDt
            fireMeter = (fireballTimeRemaining / fireballActiveDuration).coerceIn(0f, 1f)
            if (fireballTimeRemaining <= 0f) {
                isFireball = false
                fireMeter = 0f
                fireballTimeRemaining = 0f
                continuousSmashTimer = 0f
            }
        } else if (!isPressing) {
            // Cool down charge meter when not pressing
            continuousSmashTimer = (continuousSmashTimer - clampedDt * 1.8f).coerceAtLeast(0f)
            fireMeter = (continuousSmashTimer / holdDurationForFireball).coerceIn(0f, 1f)
            comboStreak = 1
        }

        // Target surface Y for current active platform
        val targetSurfaceY = if (activeLayerIndex < layers.size) {
            layers[activeLayerIndex].y
        } else {
            totalLayers * layerSpacing // Checkered finish base
        }

        // 6. Ball Physics: Bouncing vs Smashing
        if (!isPressing) {
            // IDLE BOUNCE
            smashCooldown = 0f
            bouncePhase += clampedDt * bounceSpeed
            val sinVal = abs(sin(bouncePhase))
            ballY = targetSurfaceY - sinVal * bounceHeight

            // Dip contact: ball hits the platform surface on every single bounce dip ("her dip me tile par ak chap aigi")
            if (sinVal < 0.16f) {
                if (!hasDippedThisBounce) {
                    hasDippedThisBounce = true
                    soundManager.playBounce()
                    hapticManager.bounce()

                    // Leave paint stamp ("chhap") on the platform at the exact contact point
                    if (activeLayerIndex < layers.size) {
                        val activeLayer = layers[activeLayerIndex]
                        val localAngle = normalizeAngle(270f - activeLayer.rotationAngle)
                        activeLayer.splats.add(
                            PaintSplat(
                                localAngleDeg = localAngle,
                                radiusRatio = 0.55f,
                                size = Random.nextFloat() * 3f + 11f,
                                color = currentSkin.primaryColor.copy(alpha = 0.90f)
                            )
                        )
                        // Keep up to 20 splats on this layer, removing oldest if exceeded
                        if (activeLayer.splats.size > 20) {
                            activeLayer.splats.removeAt(0)
                        }
                    }
                }
            } else if (sinVal > 0.45f) {
                hasDippedThisBounce = false
            }
        } else {
            hasDippedThisBounce = false
            // SMASHING DOWNWARD
            // Continuous 3s hold charges and activates 3s Fireball Mode
            if (!isFireball) {
                continuousSmashTimer += clampedDt
                fireMeter = (continuousSmashTimer / holdDurationForFireball).coerceIn(0f, 1f)
                if (continuousSmashTimer >= holdDurationForFireball) {
                    isFireball = true
                    fireballTimeRemaining = fireballActiveDuration
                    continuousSmashTimer = 0f
                    fireMeter = 1.0f
                    soundManager.playFireSmash()
                    hapticManager.fireSmash()
                    screenShake = 7f
                }
            }

            if (smashCooldown > 0f) {
                smashCooldown -= clampedDt
                ballY = targetSurfaceY
            } else {
                val smashVelocity = if (isFireball) 250f else 185f
                ballY += smashVelocity * clampedDt

                // Check if ball reached the current active layer surface
                if (activeLayerIndex < layers.size && ballY >= targetSurfaceY) {
                    ballY = targetSurfaceY
                    val currentLayer = layers[activeLayerIndex]

                    // Determine which segment is directly under the ball (ball is at polar angle 270 deg)
                    val localAngle = normalizeAngle(270f - currentLayer.rotationAngle)
                    val hitSegment = currentLayer.segments.firstOrNull { seg ->
                        val segStart = normalizeAngle(seg.startAngleDeg)
                        val segEnd = segStart + seg.sweepAngleDeg
                        if (segEnd <= 360f) {
                            localAngle >= segStart && localAngle < segEnd
                        } else {
                            // Wraps around 360 degrees
                            localAngle >= segStart || localAngle < (segEnd - 360f)
                        }
                    } ?: currentLayer.segments.minByOrNull { seg ->
                        val center = normalizeAngle(seg.startAngleDeg + seg.sweepAngleDeg * 0.5f)
                        val diff = abs(localAngle - center)
                        if (diff > 180f) 360f - diff else diff
                    }

                    val isDangerHit = hitSegment?.isDanger == true

                    if (isDangerHit && !isFireball) {
                        // FATAL COLLISION! (Normal mode hitting black obstacle)
                        playState = PlayState.GAME_OVER
                        isPressing = false
                        screenShake = 16f
                        soundManager.playGameOver()
                        hapticManager.fail()
                        // Spawn shattering ball fragments
                        spawnBallDeathFragments(currentSkin)
                        // Update High Score
                        if (score > highScore) {
                            highScore = score
                            prefs.highScore = score
                        }
                    } else {
                        // SUCCESSFUL SMASH! (In Fireball mode, even danger segments are shattered!)
                        currentLayer.isBroken = true
                        spawnLayerShatterDebris(currentLayer, isDangerHit || isFireball)

                        val pointsEarned = if (isFireball) 25 else (comboStreak * 2).coerceAtMost(20)
                        score += pointsEarned
                        if (score > highScore) {
                            highScore = score
                            prefs.highScore = score
                        }
                        comboStreak++

                        if (isFireball) {
                            soundManager.playFireSmash()
                            hapticManager.fireSmash()
                            screenShake = 7f
                        } else {
                            soundManager.playSmash(comboStreak)
                            hapticManager.smash()
                            screenShake = 3.5f
                        }

                        // Advance to next layer with a small impact cadence
                        activeLayerIndex++
                        smashCooldown = 0.05f

                        // Check if all layers cleared (reached checkered base)!
                        if (activeLayerIndex >= totalLayers) {
                            ballY = totalLayers * layerSpacing
                            playState = PlayState.LEVEL_COMPLETED
                            isPressing = false
                            soundManager.playWin()
                            hapticManager.win()
                            val levelCoins = 25 + currentLevel * 5
                            addCoins(levelCoins)
                        }
                    }
                } else if (activeLayerIndex >= totalLayers && ballY >= targetSurfaceY) {
                    // Reached checkered finish base
                    ballY = targetSurfaceY
                    if (playState != PlayState.LEVEL_COMPLETED) {
                        playState = PlayState.LEVEL_COMPLETED
                        isPressing = false
                        soundManager.playWin()
                        hapticManager.win()
                        val levelCoins = 25 + currentLevel * 5
                        addCoins(levelCoins)
                    }
                }
            }
        }

        // 7. Smooth Camera Vertical Tracking
        val targetCamY = ballY - 45f
        camY += (targetCamY - camY) * (clampedDt * 10f)
    }

    private fun spawnLayerShatterDebris(layer: PlatformLayer, isFireSmashOnDanger: Boolean) {
        val rnd = Random(System.nanoTime())
        val layerCenterY = layer.y + 7f

        // 1. Spawn Break Shockwave Ring at smash plane
        val shockColor = if (isFireSmashOnDanger || isFireball) {
            Color(0xFFFF6D00)
        } else {
            currentTheme.platformHighlightColor
        }
        shockwaves.add(
            BreakShockwave(
                y = layer.y,
                color = shockColor
            )
        )
        while (shockwaves.size > 5) {
            shockwaves.removeAt(0)
        }

        // 2. Break every segment of this layer into realistic flying 3D tile pieces!
        for (seg in layer.segments) {
            val segStartDeg = seg.startAngleDeg + layer.rotationAngle
            val segSweepDeg = seg.sweepAngleDeg

            val isSegDanger = seg.isDanger
            val tileColor = if (isFireSmashOnDanger || isFireball) {
                if (rnd.nextBoolean()) Color(0xFFFF3D00) else Color(0xFFFFEA00)
            } else if (isSegDanger) {
                Color(0xFF1E2024)
            } else {
                currentTheme.platformColor
            }
            val tileHighlight = if (isFireSmashOnDanger || isFireball) {
                Color(0xFFFFF176)
            } else if (isSegDanger) {
                Color(0xFF37474F)
            } else {
                currentTheme.platformHighlightColor
            }

            // Subdivide segment into 2 substantial flying tile chunks
            val chunksPerSeg = 2
            for (c in 0 until chunksPerSeg) {
                val subMidDeg = segStartDeg + (c + 0.5f) * (segSweepDeg / chunksPerSeg)
                val subMidRad = Math.toRadians(subMidDeg.toDouble()).toFloat()

                // Radial explosion trajectory (flying away from center pole)
                val radialDist = 58f
                val startX = cos(subMidRad) * radialDist
                val startZ = sin(subMidRad) * radialDist

                // Lateral sideways explosion: pieces fly out sideways (left/right) from center,
                // avoiding flying straight forward into user camera view for clear visibility!
                val sideSign = if (cos(subMidRad) >= 0f) 1f else -1f
                val sideSpeed = rnd.nextFloat() * 180f + 260f // 260 .. 440 px/s lateral
                val vx = sideSign * sideSpeed + (rnd.nextFloat() * 60f - 30f)
                // Keep Z neutral or gently pushing away (backward) so fragments don't fly at user's face
                val vz = (rnd.nextFloat() * 60f + 20f) // pushes away into background
                val vy = -(rnd.nextFloat() * 110f + 70f) // light upward arc before dropping

                debrisList.add(
                    DebrisFragment(
                        x = startX,
                        y = layerCenterY,
                        z = startZ,
                        vx = vx,
                        vy = vy,
                        vz = vz,
                        rotX = rnd.nextFloat() * 360f,
                        rotY = subMidDeg,
                        rotZ = rnd.nextFloat() * 360f,
                        vRotX = rnd.nextFloat() * 700f - 350f,
                        vRotY = rnd.nextFloat() * 700f - 350f,
                        vRotZ = rnd.nextFloat() * 700f - 350f,
                        sizeX = rnd.nextFloat() * 8f + 26f,
                        sizeY = 14f,
                        sizeZ = rnd.nextFloat() * 8f + 20f,
                        color = tileColor,
                        highlightColor = tileHighlight,
                        alpha = 1f,
                        life = rnd.nextFloat() * 0.4f + 1.2f,
                        isTileChunk = true,
                        shape = layer.shape
                    )
                )
            }

            // Also spawn 2 smaller splinter crumb pieces for each segment (flying laterally sideways)
            val crumbsCount = 2
            for (k in 0 until crumbsCount) {
                val crumbAngleRad = Math.toRadians((segStartDeg + rnd.nextFloat() * segSweepDeg).toDouble()).toFloat()
                val crumbDist = rnd.nextFloat() * 25f + 45f
                val crumbSideSign = if (cos(crumbAngleRad) >= 0f) 1f else -1f
                val crumbSpeed = rnd.nextFloat() * 200f + 300f
                val vx = crumbSideSign * crumbSpeed
                val vz = rnd.nextFloat() * 50f + 10f // sideways/backward, not at user
                val vy = -(rnd.nextFloat() * 130f + 70f)

                debrisList.add(
                    DebrisFragment(
                        x = cos(crumbAngleRad) * crumbDist,
                        y = layerCenterY,
                        z = sin(crumbAngleRad) * crumbDist,
                        vx = vx,
                        vy = vy,
                        vz = vz,
                        rotX = rnd.nextFloat() * 360f,
                        rotY = rnd.nextFloat() * 360f,
                        rotZ = rnd.nextFloat() * 360f,
                        vRotX = rnd.nextFloat() * 800f - 400f,
                        vRotY = rnd.nextFloat() * 800f - 400f,
                        vRotZ = rnd.nextFloat() * 800f - 400f,
                        sizeX = rnd.nextFloat() * 6f + 7f,
                        sizeY = rnd.nextFloat() * 6f + 7f,
                        sizeZ = 8f,
                        color = if (rnd.nextBoolean()) tileHighlight else tileColor,
                        highlightColor = tileHighlight,
                        alpha = 1f,
                        life = rnd.nextFloat() * 0.3f + 0.9f,
                        isTileChunk = false,
                        shape = layer.shape
                    )
                )
            }
        }
        while (debrisList.size > 50) {
            debrisList.removeAt(0)
        }
    }

    private fun spawnBallDeathFragments(skin: BallSkin) {
        val rnd = Random(42)
        val numPieces = 22
        for (i in 0 until numPieces) {
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val speed = rnd.nextFloat() * 180f + 80f
            debrisList.add(
                DebrisFragment(
                    x = cos(angle) * 8f,
                    y = ballY,
                    z = -48f + sin(angle) * 8f,
                    vx = cos(angle) * speed,
                    vy = -(rnd.nextFloat() * 160f + 80f),
                    vz = sin(angle) * speed,
                    sizeX = rnd.nextFloat() * 8f + 5f,
                    sizeY = rnd.nextFloat() * 8f + 5f,
                    color = if (rnd.nextBoolean()) skin.primaryColor else skin.highlightColor,
                    vRotX = rnd.nextFloat() * 500f,
                    vRotY = rnd.nextFloat() * 500f,
                    vRotZ = rnd.nextFloat() * 500f
                )
            )
        }
    }

    fun getScreenShakeOffset(): Offset {
        if (screenShake <= 0f) return Offset.Zero
        val rnd = Random(System.nanoTime())
        val ox = (rnd.nextFloat() * 2f - 1f) * screenShake
        val oy = (rnd.nextFloat() * 2f - 1f) * screenShake
        return Offset(ox, oy)
    }
}
