package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.engine.StackBallGameEngine
import com.example.engine.StackBallRenderer
import com.example.model.AVAILABLE_BALL_SKINS
import com.example.model.PlayState
import com.example.util.HapticManager

@Composable
fun StackBallGameScreen() {
    val context = LocalContext.current
    val prefs = remember { GamePreferences(context) }
    var soundEnabled by remember { mutableStateOf(prefs.isSoundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(prefs.isHapticsEnabled) }

    val soundManager = remember {
        SoundManager(isSoundEnabled = { soundEnabled })
    }
    val hapticManager = remember {
        HapticManager(context, isHapticsEnabled = { hapticsEnabled })
    }

    val engine = remember {
        StackBallGameEngine(prefs, soundManager, hapticManager)
    }
    val renderer = remember { StackBallRenderer() }

    // Screen navigation state (starts on Main Menu)
    var isAtMainMenu by remember { mutableStateOf(true) }

    // Dialog state
    var showShop by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Selected ball skin
    var selectedSkinId by remember { mutableStateOf(prefs.selectedSkinId) }
    var unlockedSkins by remember { mutableStateOf(prefs.unlockedSkinIds) }

    val currentSkin = remember(selectedSkinId) {
        AVAILABLE_BALL_SKINS.find { it.id == selectedSkinId } ?: AVAILABLE_BALL_SKINS.first()
    }

    // High performance frame loop
    var lastNanoTime by remember { mutableLongStateOf(0L) }
    var frameTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        lastNanoTime = System.nanoTime()
        while (true) {
            withFrameNanos { nowNanos ->
                val dt = if (lastNanoTime == 0L) 0.016f else ((nowNanos - lastNanoTime) / 1_000_000_000f)
                lastNanoTime = nowNanos
                engine.update(dt, currentSkin)
                frameTrigger = nowNanos
            }
        }
    }

    // Pulsing animation for "HOLD TO SMASH" and "TOUCH TO NEXT"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Lifecycle Observer: Music plays only during game, pauses on minimize/background/ads/lock, resumes on return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!isAtMainMenu && !showShop && !showSettings && soundEnabled) {
                        soundManager.resumeMusic()
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    soundManager.pauseMusic()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    soundManager.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundManager.pauseMusic()
        }
    }

    BackHandler(enabled = true) {
        if (showShop) {
            showShop = false
            if (!isAtMainMenu && soundEnabled) soundManager.resumeMusic()
        } else if (showSettings) {
            showSettings = false
            if (!isAtMainMenu && soundEnabled) soundManager.resumeMusic()
        } else if (!isAtMainMenu) {
            isAtMainMenu = true
            soundManager.stopMusic()
            engine.retryLevel()
        }
    }

    // Main Content
    if (isAtMainMenu) {
        MainMenuScreen(
            currentLevel = engine.currentLevel,
            highScore = engine.highScore,
            coins = engine.coins,
            equippedSkin = currentSkin,
            theme = engine.currentTheme,
            ambientParticles = engine.ambientParticles,
            isSoundEnabled = soundEnabled,
            onToggleSound = {
                soundEnabled = !soundEnabled
                prefs.isSoundEnabled = soundEnabled
                soundManager.playClick()
                if (!soundEnabled) {
                    soundManager.pauseMusic()
                }
            },
            onPlayClick = {
                soundManager.playGameStart()
                engine.startLevel(engine.currentLevel)
                isAtMainMenu = false
                if (soundEnabled) {
                    soundManager.startMusic()
                }
            },
            onOpenSkinStore = {
                soundManager.playClick()
                showShop = true
            }
        )
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stack_ball_game_container")
        ) {
            val screenWidth = constraints.maxWidth.toFloat()
            val screenHeight = constraints.maxHeight.toFloat()
            val theme = engine.currentTheme

            // 1. Dynamic 3D Perspective Canvas with Touch Tracking
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(engine.playState) {
                        // Only intercept gameplay touches when playing or waiting to start
                        if (engine.playState != PlayState.WAITING_TO_START && engine.playState != PlayState.PLAYING) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            if (engine.playState == PlayState.WAITING_TO_START || engine.playState == PlayState.PLAYING) {
                                engine.isPressing = true
                                waitForUpOrCancellation()
                                engine.isPressing = false
                            }
                        }
                    }
                    .testTag("game_canvas")
            ) {
                // Read frameTrigger to recompose each frame
                @Suppress("UNUSED_VARIABLE")
                val frame = frameTrigger

                // Background vertical gradient matching the level theme
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(theme.bgTopColor, theme.bgBottomColor)
                    )
                )

                // Render 3D Stack Ball world with screen shake
                val shake = engine.getScreenShakeOffset()
                translate(left = shake.x, top = shake.y) {
                    renderer.renderScene(
                        scope = this,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        theme = theme,
                        layers = engine.layers,
                        activeLayerIndex = engine.activeLayerIndex,
                        totalLayers = engine.totalLayers,
                        layerSpacing = engine.layerSpacing,
                        ballY = engine.ballY,
                        camY = engine.camY,
                        skin = currentSkin,
                        isSmashing = engine.isPressing,
                        isFireball = engine.isFireball,
                        fireMeter = engine.fireMeter,
                        debris = engine.debrisList,
                        shockwaves = engine.shockwaves,
                        ambientParticles = engine.ambientParticles,
                        playState = engine.playState,
                        screenShakeOffset = shake
                    )
                }
            }

            // 2. Heads-Up Display (Top Bar, Level Progress, Score, Home Button)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                StackBallHUD(
                    level = engine.currentLevel,
                    activeLayerIndex = engine.activeLayerIndex,
                    totalLayers = engine.totalLayers,
                    score = engine.score,
                    fireMeter = engine.fireMeter,
                    isFireball = engine.isFireball,
                    shape = theme.primaryShape,
                    onHomeClick = {
                        soundManager.playClick()
                        soundManager.stopMusic()
                        isAtMainMenu = true
                    }
                )
            }

            // 3. In-Game State Overlays
            when (engine.playState) {
                PlayState.WAITING_TO_START -> {
                    // Pulsing initial hint
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "HOLD TO SMASH",
                            color = Color.White.copy(alpha = pulseAlpha),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .scale(pulseScale)
                                .testTag("hold_to_smash_hint")
                        )
                    }
                }

                PlayState.LEVEL_COMPLETED -> {
                    // Level Completed Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x22000000))
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(0.3f))

                            Text(
                                text = "Level ${engine.currentLevel}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "COMPLETED!",
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bonus coins badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0x88000000), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Bonus Coins",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = " +${25 + engine.currentLevel * 5} COINS",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.weight(0.5f))

                            Button(
                                onClick = {
                                    soundManager.playGameStart()
                                    engine.nextLevel()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(56.dp)
                                    .scale(pulseScale)
                                    .testTag("touch_to_next_button")
                            ) {
                                Text(
                                    text = "TOUCH TO NEXT",
                                    color = Color.Black,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    soundManager.playClick()
                                    soundManager.stopMusic()
                                    engine.nextLevel()
                                    isAtMainMenu = true
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "MAIN MENU",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }

                PlayState.GAME_OVER -> {
                    // Game Over Overlay Popup
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp)
                                .testTag("game_over_card")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "GAME OVER",
                                    color = Color(0xFFFF3D00),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Black obstacle hit!",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "SCORE",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${engine.score}",
                                    color = Color.White,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Best Score",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BEST: ${engine.highScore}",
                                        color = Color(0xFFFFD700),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        soundManager.playClick()
                                        engine.retryLevel()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("retry_level_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Retry",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TRY AGAIN",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        soundManager.playClick()
                                        soundManager.stopMusic()
                                        isAtMainMenu = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("game_over_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Menu",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MAIN MENU",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }

    // 4. Shop Dialog (Globally displayed when requested from either Main Menu or in-game)
    if (showShop) {
        ShopDialog(
            coins = engine.coins,
            selectedSkinId = selectedSkinId,
            unlockedSkinIds = unlockedSkins,
            onSelectSkin = { skinId ->
                selectedSkinId = skinId
                prefs.selectedSkinId = skinId
                soundManager.playClick()
            },
            onUnlockSkin = { skin ->
                if (engine.coins >= skin.cost) {
                    engine.addCoins(-skin.cost)
                    prefs.unlockSkin(skin.id)
                    unlockedSkins = prefs.unlockedSkinIds
                    selectedSkinId = skin.id
                    prefs.selectedSkinId = skin.id
                    soundManager.playUnlock()
                }
            },
            onDismiss = {
                showShop = false
                soundManager.playClick()
                if (!isAtMainMenu && soundEnabled) {
                    soundManager.resumeMusic()
                }
            }
        )
    }

    // 5. Settings Dialog (Globally displayed when requested)
    if (showSettings) {
        SettingsDialog(
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
            onToggleSound = {
                soundEnabled = it
                prefs.isSoundEnabled = it
                soundManager.playClick()
                if (it && !isAtMainMenu) {
                    soundManager.startMusic()
                } else if (!it) {
                    soundManager.pauseMusic()
                }
            },
            onToggleHaptics = {
                hapticsEnabled = it
                prefs.isHapticsEnabled = it
                soundManager.playClick()
            },
            onDismiss = {
                showSettings = false
                soundManager.playClick()
                if (!isAtMainMenu && soundEnabled) {
                    soundManager.resumeMusic()
                }
            }
        )
    }
}
