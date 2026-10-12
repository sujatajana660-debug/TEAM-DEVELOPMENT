package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.AmbientParticle
import com.example.model.BallSkin
import com.example.model.BreakShockwave
import com.example.model.DebrisFragment
import com.example.model.LevelTheme
import com.example.model.PlatformLayer
import com.example.model.PlatformShape
import com.example.model.PlayState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class StackBallRenderer {

    // 3D Geometry Constants
    val cylinderRadius = 24f
    val platformInnerRadius = 0f
    val platformOuterRadius = 78f
    val platformThickness = 14f
    val ballRadius = 14f
    val ballOrbitZ = -48f // Ball is positioned on the front surface of the platform
    val baseRadius = 92f

    // Camera parameters
    private val cameraPitch = 24f * (PI.toFloat() / 180f) // 24 degrees tilt down
    private val cosPitch = cos(cameraPitch)
    private val sinPitch = sin(cameraPitch)
    private val camDistZ = -260f
    private val focalLength = 420f

    data class ProjectedPoint(
        val screenX: Float,
        val screenY: Float,
        val depth: Float,
        val scale: Float
    )

    fun project(
        x: Float,
        y: Float,
        z: Float,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ): ProjectedPoint {
        val dx = x
        val dy = y - camY
        val dz = z - camDistZ

        val rotY = dy * cosPitch - dz * sinPitch
        val rotZ = dy * sinPitch + dz * cosPitch

        val depth = rotZ.coerceAtLeast(15f)
        // Automatic responsive scaling across all screen sizes (phones, tablets, foldables)
        val refDimension = minOf(screenWidth, screenHeight * 0.52f)
        val scaleFactor = (refDimension / 380f).coerceIn(0.70f, 1.40f)
        val scale = (focalLength / depth) * scaleFactor

        val screenX = screenWidth * 0.5f + dx * scale
        val screenY = screenHeight * 0.44f + rotY * scale

        return ProjectedPoint(screenX, screenY, depth, scale)
    }

    /**
     * Computes the outer radius for various platform shapes (Circle, Hexagon, Star, Flower, Octagon, Square)
     */
    fun getOuterRadius(shape: PlatformShape, angleRad: Float): Float {
        return when (shape) {
            PlatformShape.CIRCLE -> platformOuterRadius
            PlatformShape.HEXAGON -> {
                // 6-sided regular polygon prism
                val step = (2.0 * PI / 6.0).toFloat()
                val mod = ((angleRad % step) + step) % step
                val alpha = mod - step * 0.5f
                (83f * cos(PI.toFloat() / 6f)) / cos(alpha).coerceAtLeast(0.65f)
            }
            PlatformShape.OCTAGON -> {
                // 8-sided regular polygon prism
                val step = (2.0 * PI / 8.0).toFloat()
                val mod = ((angleRad % step) + step) % step
                val alpha = mod - step * 0.5f
                (82f * cos(PI.toFloat() / 8f)) / cos(alpha).coerceAtLeast(0.72f)
            }
            PlatformShape.FLOWER -> {
                // 6 rounded flower lobes / petals
                73f + 11f * cos(6f * angleRad)
            }
            PlatformShape.STAR -> {
                // 8-point sharp star cogwheel
                val c = cos(8f * angleRad)
                69f + 15f * (c * c * c)
            }
            PlatformShape.SQUARE -> {
                // 4-sided diamond / square prism
                val step = (2.0 * PI / 4.0).toFloat()
                val mod = ((angleRad % step) + step) % step
                val alpha = mod - step * 0.5f
                (85f * cos(PI.toFloat() / 4f)) / cos(alpha).coerceAtLeast(0.70f)
            }
        }
    }

    fun renderScene(
        scope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        theme: LevelTheme,
        layers: List<PlatformLayer>,
        activeLayerIndex: Int,
        totalLayers: Int,
        layerSpacing: Float,
        ballY: Float,
        camY: Float,
        skin: BallSkin,
        isSmashing: Boolean,
        isFireball: Boolean,
        fireMeter: Float,
        debris: List<DebrisFragment>,
        shockwaves: List<BreakShockwave> = emptyList(),
        ambientParticles: List<AmbientParticle>,
        playState: PlayState,
        screenShakeOffset: Offset
    ) {
        val finishBaseY = totalLayers * layerSpacing

        // 1. Draw Ambient Floating Bokeh Particles
        drawAmbientParticles(scope, ambientParticles, screenWidth, screenHeight)

        // 2. Draw Checkered Finish Base (if near camera)
        drawCheckeredBase(scope, finishBaseY, camY, screenWidth, screenHeight)

        // (Pipe/Pole removed as requested: platforms are solid through center and tiles break outward to sides)

        // 4. Draw Platform Layers (Smart Viewport & Frustum Culling to prevent phone lag on large levels)
        val visibleMinLayer = (activeLayerIndex - 2).coerceAtLeast(0)
        val visibleMaxLayer = (activeLayerIndex + 18).coerceAtMost(layers.size - 1)

        for (idx in visibleMaxLayer downTo visibleMinLayer) {
            val layer = layers[idx]
            if (!layer.isBroken) {
                // Pre-check viewport visibility: skip drawing layers that fall offscreen
                val projCenter = project(0f, layer.y, 0f, camY, screenWidth, screenHeight)
                if (projCenter.screenY in -120f..(screenHeight + 120f)) {
                    drawPlatformLayer(scope, layer, theme, camY, screenWidth, screenHeight)
                }
            }
        }

        // 5. Draw Break Shockwaves
        if (shockwaves.isNotEmpty()) {
            drawShockwaves(scope, shockwaves, camY, screenWidth, screenHeight)
        }

        // 6. Draw Shattered 3D Debris Fragments (Realistic tumbling broken tiles!)
        drawDebris(scope, debris, camY, screenWidth, screenHeight)

        // 7. Draw Player Ball and FX (if not dead)
        if (playState != PlayState.GAME_OVER) {
            drawBallAndTrail(
                scope,
                ballY,
                camY,
                screenWidth,
                screenHeight,
                skin,
                isSmashing,
                isFireball,
                fireMeter
            )
        }
    }

    private fun drawAmbientParticles(
        scope: DrawScope,
        particles: List<AmbientParticle>,
        screenWidth: Float,
        screenHeight: Float
    ) {
        for (p in particles) {
            val cx = (p.x * screenWidth)
            val cy = (p.y * screenHeight)
            val col = Color.White.copy(alpha = p.alpha)
            scope.drawCircle(
                color = col,
                radius = p.radius,
                center = Offset(cx, cy)
            )
        }
    }

    private fun drawPlatformLayer(
        scope: DrawScope,
        layer: PlatformLayer,
        theme: LevelTheme,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val yTop = layer.y
        val yBot = layer.y + platformThickness
        val rotDeg = layer.rotationAngle

        // Draw each segment of this disc.
        // To maintain clean depth ordering around the circle, we sort segments by sin
        // higher sin (back of tower) drawn first so front faces cleanly overlap back faces
        val sortedSegments = layer.segments.sortedByDescending { seg ->
            val midA = Math.toRadians((seg.startAngleDeg + seg.sweepAngleDeg * 0.5f + rotDeg).toDouble())
            sin(midA)
        }

        // Two-pass rendering for clean geometry:
        // Pass 1: Draw outer rim faces for all visible forward-facing segments
        for (seg in sortedSegments) {
            drawSegmentRims(
                scope,
                yTop,
                yBot,
                seg.startAngleDeg + rotDeg,
                seg.sweepAngleDeg,
                seg.isDanger,
                theme,
                layer.shape,
                camY,
                screenWidth,
                screenHeight
            )
        }

        // Pass 2: Draw top horizontal surface faces
        for (seg in sortedSegments) {
            drawSegmentTopFace(
                scope,
                yTop,
                seg.startAngleDeg + rotDeg,
                seg.sweepAngleDeg,
                seg.isDanger,
                theme,
                layer.shape,
                camY,
                screenWidth,
                screenHeight
            )
        }

        // Draw paint splats / stamps ("chhap") on this layer directly under the ball contact point
        if (layer.splats.isNotEmpty()) {
            for (splat in layer.splats) {
                // Ball hits the platform at x=0, z=ballOrbitZ.
                // In local layer space rotated by rotDeg:
                val localAngleRad = Math.toRadians(splat.localAngleDeg.toDouble())
                val totalAngleRad = localAngleRad + Math.toRadians(rotDeg.toDouble())
                // Use ballOrbitZ radius so the splat is exactly directly under the bouncing ball!
                val splatRadius = Math.abs(ballOrbitZ)
                val sx = (splatRadius * cos(totalAngleRad)).toFloat()
                val sz = (splatRadius * sin(totalAngleRad)).toFloat()

                val proj = project(sx, yTop - 0.2f, sz, camY, screenWidth, screenHeight)
                val baseR = splat.size * proj.scale

                // 1. Main paint imprint blob
                scope.drawCircle(
                    color = splat.color,
                    radius = baseR,
                    center = Offset(proj.screenX, proj.screenY)
                )

                // 2. Inner paint stamp specular highlight
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = baseR * 0.42f,
                    center = Offset(proj.screenX - baseR * 0.16f, proj.screenY - baseR * 0.16f)
                )

                // 3. Small splash droplet spots around the stamp
                val dOffset = baseR * 1.35f
                scope.drawCircle(
                    color = splat.color.copy(alpha = 0.75f),
                    radius = baseR * 0.22f,
                    center = Offset(proj.screenX + dOffset * 0.7f, proj.screenY - dOffset * 0.5f)
                )
                scope.drawCircle(
                    color = splat.color.copy(alpha = 0.65f),
                    radius = baseR * 0.18f,
                    center = Offset(proj.screenX - dOffset * 0.6f, proj.screenY + dOffset * 0.6f)
                )
            }
        }
    }

    private fun drawSegmentRims(
        scope: DrawScope,
        yTop: Float,
        yBot: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        isDanger: Boolean,
        theme: LevelTheme,
        shape: PlatformShape,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val steps = 4
        val stepSweep = sweepAngleDeg / steps

        val darkSideColor = if (isDanger) Color(0xFF141518) else theme.platformColor.copy(
            red = theme.platformColor.red * 0.72f,
            green = theme.platformColor.green * 0.72f,
            blue = theme.platformColor.blue * 0.72f
        )

        for (i in 0 until steps) {
            val a1 = Math.toRadians((startAngleDeg + i * stepSweep).toDouble()).toFloat()
            val a2 = Math.toRadians((startAngleDeg + (i + 1) * stepSweep).toDouble()).toFloat()

            val cos1 = cos(a1)
            val sin1 = sin(a1)
            val cos2 = cos(a2)
            val sin2 = sin(a2)

            val rOuter1 = getOuterRadius(shape, a1)
            val rOuter2 = getOuterRadius(shape, a2)

            val midSin = (sin1 + sin2) * 0.5f
            // Only draw outer rim faces if facing front / camera
            if (midSin <= 0.15f) {
                val pOuterTop1 = project(rOuter1 * cos1, yTop, rOuter1 * sin1, camY, screenWidth, screenHeight)
                val pOuterTop2 = project(rOuter2 * cos2, yTop, rOuter2 * sin2, camY, screenWidth, screenHeight)
                val pOuterBot1 = project(rOuter1 * cos1, yBot, rOuter1 * sin1, camY, screenWidth, screenHeight)
                val pOuterBot2 = project(rOuter2 * cos2, yBot, rOuter2 * sin2, camY, screenWidth, screenHeight)

                val outerRimPath = Path().apply {
                    moveTo(pOuterTop1.screenX, pOuterTop1.screenY)
                    lineTo(pOuterTop2.screenX, pOuterTop2.screenY)
                    lineTo(pOuterBot2.screenX, pOuterBot2.screenY)
                    lineTo(pOuterBot1.screenX, pOuterBot1.screenY)
                    close()
                }

                // Normal lighting based on angle
                val lightFactor = (0.55f + 0.45f * ((-sin1 - sin2) * 0.5f)).coerceIn(0.4f, 1.0f)
                val rimColor = Color(
                    red = darkSideColor.red * lightFactor,
                    green = darkSideColor.green * lightFactor,
                    blue = darkSideColor.blue * lightFactor,
                    alpha = 1f
                )
                scope.drawPath(outerRimPath, rimColor)
            }
        }
    }

    private fun drawSegmentTopFace(
        scope: DrawScope,
        yTop: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        isDanger: Boolean,
        theme: LevelTheme,
        shape: PlatformShape,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val steps = 4
        val stepSweep = sweepAngleDeg / steps

        val mainColor = if (isDanger) theme.dangerColor else theme.platformColor
        val highlightColor = if (isDanger) Color(0xFF2C2F36) else theme.platformHighlightColor

        for (i in 0 until steps) {
            val a1 = Math.toRadians((startAngleDeg + i * stepSweep).toDouble()).toFloat()
            val a2 = Math.toRadians((startAngleDeg + (i + 1) * stepSweep).toDouble()).toFloat()

            val cos1 = cos(a1)
            val sin1 = sin(a1)
            val cos2 = cos(a2)
            val sin2 = sin(a2)

            val rOuter1 = getOuterRadius(shape, a1)
            val rOuter2 = getOuterRadius(shape, a2)

            // Inner surface points (center point for solid fill)
            val pInnerTop1 = project(platformInnerRadius * cos1, yTop, platformInnerRadius * sin1, camY, screenWidth, screenHeight)
            val pInnerTop2 = project(platformInnerRadius * cos2, yTop, platformInnerRadius * sin2, camY, screenWidth, screenHeight)

            // Outer surface points
            val pOuterTop1 = project(rOuter1 * cos1, yTop, rOuter1 * sin1, camY, screenWidth, screenHeight)
            val pOuterTop2 = project(rOuter2 * cos2, yTop, rOuter2 * sin2, camY, screenWidth, screenHeight)

            val midSin = (sin1 + sin2) * 0.5f

            // Draw Top horizontal surface face (closed quad)
            val topFacePath = Path().apply {
                moveTo(pInnerTop1.screenX, pInnerTop1.screenY)
                lineTo(pOuterTop1.screenX, pOuterTop1.screenY)
                lineTo(pOuterTop2.screenX, pOuterTop2.screenY)
                lineTo(pInnerTop2.screenX, pInnerTop2.screenY)
                close()
            }

            // Subtle gradient/specular on top face
            val topFaceColor = if (midSin < 0f) highlightColor else mainColor
            scope.drawPath(topFacePath, topFaceColor)

            // Crisp facet seam stroke
            scope.drawPath(
                topFacePath,
                color = if (isDanger) Color(0xFF101114) else mainColor.copy(alpha = 0.45f),
                style = Stroke(width = 1.2f)
            )
        }
    }

    private fun drawShockwaves(
        scope: DrawScope,
        shockwaves: List<BreakShockwave>,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val steps = 20
        for (sw in shockwaves) {
            val path = Path()
            for (i in 0..steps) {
                val a = (i.toFloat() / steps) * 2f * PI.toFloat()
                val px = cos(a) * sw.radius
                val pz = sin(a) * sw.radius
                val proj = project(px, sw.y, pz, camY, screenWidth, screenHeight)
                if (i == 0) {
                    path.moveTo(proj.screenX, proj.screenY)
                } else {
                    path.lineTo(proj.screenX, proj.screenY)
                }
            }
            scope.drawPath(
                path = path,
                color = sw.color.copy(alpha = sw.alpha.coerceIn(0f, 1f)),
                style = Stroke(width = 3.5f)
            )
        }
    }

    private fun drawCheckeredBase(
        scope: DrawScope,
        baseY: Float,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val numSegments = 16
        val sweep = 360f / numSegments

        for (i in 0 until numSegments) {
            val a1 = Math.toRadians((i * sweep).toDouble())
            val a2 = Math.toRadians(((i + 1) * sweep).toDouble())

            val cos1 = cos(a1).toFloat()
            val sin1 = sin(a1).toFloat()
            val cos2 = cos(a2).toFloat()
            val sin2 = sin(a2).toFloat()

            val pIn1 = project(0f, baseY, 0f, camY, screenWidth, screenHeight)
            val pOut1 = project(baseRadius * cos1, baseY, baseRadius * sin1, camY, screenWidth, screenHeight)
            val pOut2 = project(baseRadius * cos2, baseY, baseRadius * sin2, camY, screenWidth, screenHeight)

            val pRimBot1 = project(baseRadius * cos1, baseY + 18f, baseRadius * sin1, camY, screenWidth, screenHeight)
            val pRimBot2 = project(baseRadius * cos2, baseY + 18f, baseRadius * sin2, camY, screenWidth, screenHeight)

            // Rim
            if ((sin1 + sin2) * 0.5f <= 0.1f) {
                val rimPath = Path().apply {
                    moveTo(pOut1.screenX, pOut1.screenY)
                    lineTo(pOut2.screenX, pOut2.screenY)
                    lineTo(pRimBot2.screenX, pRimBot2.screenY)
                    lineTo(pRimBot1.screenX, pRimBot1.screenY)
                    close()
                }
                scope.drawPath(rimPath, Color(0xFFE0E0E0))
            }

            // Top check tile
            val isWhite = (i % 2 == 0)
            val tileColor = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF263238)

            val tilePath = Path().apply {
                moveTo(pIn1.screenX, pIn1.screenY)
                lineTo(pOut1.screenX, pOut1.screenY)
                lineTo(pOut2.screenX, pOut2.screenY)
                close()
            }
            scope.drawPath(tilePath, tileColor)
        }
    }

    /**
     * Renders shattered 3D debris fragments tumbling in space with full perspective and lighting
     */
    private fun drawDebris(
        scope: DrawScope,
        debrisList: List<DebrisFragment>,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        for (d in debrisList) {
            val currentAlpha = (d.alpha * (d.life / 1.4f)).coerceIn(0f, 1f)

            if (d.isTileChunk) {
                // Real 3D Tumbling Tile Chunk (Box / Wedge with 8 vertices & 6 faces)
                val hw = d.sizeX * 0.5f
                val hh = d.sizeY * 0.5f
                val hd = d.sizeZ * 0.5f

                val radX = Math.toRadians(d.rotX.toDouble()).toFloat()
                val radY = Math.toRadians(d.rotY.toDouble()).toFloat()
                val radZ = Math.toRadians(d.rotZ.toDouble()).toFloat()

                val cx = cos(radX); val sx = sin(radX)
                val cy = cos(radY); val sy = sin(radY)
                val cz = cos(radZ); val sz = sin(radZ)

                // Rotation matrix: R = Ry * Rx * Rz
                val r00 = cy * cz + sy * sx * sz
                val r01 = -cy * sz + sy * sx * cz
                val r02 = sy * cx

                val r10 = cx * sz
                val r11 = cx * cz
                val r12 = -sx

                val r20 = -sy * cz + cy * sx * sz
                val r21 = sy * sz + cy * sx * cz
                val r22 = cy * cx

                // 8 local vertices:
                val localVerts = arrayOf(
                    floatArrayOf(-hw, -hh, -hd), // 0: top-back-left
                    floatArrayOf(hw, -hh, -hd),  // 1: top-back-right
                    floatArrayOf(hw, -hh, hd),   // 2: top-front-right
                    floatArrayOf(-hw, -hh, hd),  // 3: top-front-left
                    floatArrayOf(-hw, hh, -hd),  // 4: bot-back-left
                    floatArrayOf(hw, hh, -hd),   // 5: bot-back-right
                    floatArrayOf(hw, hh, hd),    // 6: bot-front-right
                    floatArrayOf(-hw, hh, hd)    // 7: bot-front-left
                )

                val projVerts = Array(8) { idx ->
                    val lx = localVerts[idx][0]
                    val ly = localVerts[idx][1]
                    val lz = localVerts[idx][2]

                    val wx = d.x + lx * r00 + ly * r01 + lz * r02
                    val wy = d.y + lx * r10 + ly * r11 + lz * r12
                    val wz = d.z + lx * r20 + ly * r21 + lz * r22

                    project(wx, wy, wz, camY, screenWidth, screenHeight)
                }

                // 6 Faces with indices and lighting colors
                data class ChunkFace(val indices: IntArray, val color: Color, val avgDepth: Float)

                val colMain = d.color.copy(alpha = currentAlpha)
                val colHigh = d.highlightColor.copy(alpha = currentAlpha)
                val colDark = Color(
                    red = d.color.red * 0.7f,
                    green = d.color.green * 0.7f,
                    blue = d.color.blue * 0.7f,
                    alpha = currentAlpha
                )
                val colDeep = Color(
                    red = d.color.red * 0.5f,
                    green = d.color.green * 0.5f,
                    blue = d.color.blue * 0.5f,
                    alpha = currentAlpha
                )

                fun avgDepth(vararg indices: Int): Float = indices.sumOf { projVerts[it].depth.toDouble() }.toFloat() / indices.size

                val faces = listOf(
                    ChunkFace(intArrayOf(0, 1, 2, 3), colHigh, avgDepth(0, 1, 2, 3)), // Top face
                    ChunkFace(intArrayOf(3, 2, 6, 7), colMain, avgDepth(3, 2, 6, 7)), // Front face
                    ChunkFace(intArrayOf(1, 0, 4, 5), colDeep, avgDepth(1, 0, 4, 5)), // Back face
                    ChunkFace(intArrayOf(2, 1, 5, 6), colDark, avgDepth(2, 1, 5, 6)), // Right face
                    ChunkFace(intArrayOf(0, 3, 7, 4), colDark, avgDepth(0, 3, 7, 4)), // Left face
                    ChunkFace(intArrayOf(4, 5, 6, 7), colDeep, avgDepth(4, 5, 6, 7))  // Bottom face
                ).sortedByDescending { it.avgDepth } // Painter's algorithm depth sort

                for (face in faces) {
                    val p = Path().apply {
                        moveTo(projVerts[face.indices[0]].screenX, projVerts[face.indices[0]].screenY)
                        for (k in 1 until face.indices.size) {
                            lineTo(projVerts[face.indices[k]].screenX, projVerts[face.indices[k]].screenY)
                        }
                        close()
                    }
                    scope.drawPath(p, face.color)
                    scope.drawPath(
                        p,
                        color = Color.White.copy(alpha = (0.25f * currentAlpha).coerceIn(0f, 1f)),
                        style = Stroke(width = 0.8f)
                    )
                }
            } else {
                // Micro Splinter / Crumb Particle
                val proj = project(d.x, d.y, d.z, camY, screenWidth, screenHeight)
                val pScale = proj.scale
                val pw = d.sizeX * pScale
                val ph = d.sizeY * pScale
                val col = d.color.copy(alpha = currentAlpha)

                scope.drawCircle(
                    color = col,
                    radius = (pw * 0.5f).coerceAtLeast(1.5f),
                    center = Offset(proj.screenX, proj.screenY)
                )
            }
        }
    }

    private fun drawBallAndTrail(
        scope: DrawScope,
        ballY: Float,
        camY: Float,
        screenWidth: Float,
        screenHeight: Float,
        skin: BallSkin,
        isSmashing: Boolean,
        isFireball: Boolean,
        fireMeter: Float
    ) {
        val ballProj = project(0f, ballY, ballOrbitZ, camY, screenWidth, screenHeight)
        val bRadius = ballRadius * ballProj.scale
        val bX = ballProj.screenX
        val bY = ballProj.screenY

        // 1. Draw Speed Streak Trail (like screenshots: luminous cone trailing upward)
        if (isSmashing) {
            val trailHeight = if (isFireball) bRadius * 5.5f else bRadius * 3.8f
            val trailTopWidth = bRadius * 0.35f
            val trailBottomWidth = bRadius * 1.6f

            val trailPath = Path().apply {
                moveTo(bX - trailBottomWidth * 0.5f, bY)
                lineTo(bX - trailTopWidth * 0.5f, bY - trailHeight)
                lineTo(bX + trailTopWidth * 0.5f, bY - trailHeight)
                lineTo(bX + trailBottomWidth * 0.5f, bY)
                close()
            }

            val trailBrush = if (isFireball) {
                Brush.verticalGradient(
                    0.0f to Color(0x00FF3D00),
                    0.5f to Color(0xCCFF9100),
                    1.0f to Color(0xFFFFEA00),
                    startY = bY - trailHeight,
                    endY = bY
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color(0x00FFFFFF),
                    0.6f to skin.primaryColor.copy(alpha = 0.5f),
                    1.0f to skin.highlightColor.copy(alpha = 0.85f),
                    startY = bY - trailHeight,
                    endY = bY
                )
            }

            scope.drawPath(trailPath, trailBrush)
        }

        // 2. Draw Fire Aura / Flames (if in Fireball mode or charging)
        if (isFireball || fireMeter > 0.65f) {
            val auraRadius = bRadius * (1.3f + 0.15f * (fireMeter))
            val fireGlow = if (isFireball) Color(0xFFFF5722) else Color(0xFFFF9800)
            scope.drawCircle(
                color = fireGlow.copy(alpha = if (isFireball) 0.65f else 0.35f),
                radius = auraRadius,
                center = Offset(bX, bY)
            )
            scope.drawCircle(
                color = Color(0xFFFFEA00).copy(alpha = 0.45f),
                radius = auraRadius * 0.82f,
                center = Offset(bX, bY)
            )
        }

        // 3. Draw 3D Spherical Ball Base
        val ballGrad = if (isFireball) {
            Brush.radialGradient(
                listOf(
                    Color(0xFFFFF9C4), // White-yellow core
                    Color(0xFFFFB300), // Blazing amber
                    Color(0xFFFF3D00), // Fiery red
                    Color(0xFFBF360C)  // Dark rim
                ),
                center = Offset(bX - bRadius * 0.32f, bY - bRadius * 0.35f),
                radius = bRadius * 1.35f
            )
        } else {
            Brush.radialGradient(
                listOf(
                    skin.highlightColor,
                    skin.primaryColor,
                    skin.secondaryColor
                ),
                center = Offset(bX - bRadius * 0.32f, bY - bRadius * 0.35f),
                radius = bRadius * 1.35f
            )
        }

        // Squash & stretch elongation when smashing
        val scaleX = if (isSmashing) 0.86f else 1.0f
        val scaleY = if (isSmashing) 1.18f else 1.0f

        scope.drawOval(
            brush = ballGrad,
            topLeft = Offset(bX - bRadius * scaleX, bY - bRadius * scaleY),
            size = Size(bRadius * 2f * scaleX, bRadius * 2f * scaleY)
        )

        // 4. Crisp Specular 3D Highlight Glint
        scope.drawOval(
            color = Color.White.copy(alpha = if (isFireball) 0.95f else 0.72f),
            topLeft = Offset(bX - bRadius * 0.45f * scaleX, bY - bRadius * 0.55f * scaleY),
            size = Size(bRadius * 0.65f * scaleX, bRadius * 0.45f * scaleY)
        )
    }
}
