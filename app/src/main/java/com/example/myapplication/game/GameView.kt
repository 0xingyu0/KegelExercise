package com.example.myapplication.game

import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import com.example.myapplication.R

class GameView : View {
    private val animalType: AnimalType

    constructor(context: Context) : super(context) {
        animalType = AnimalType.DOBERMAN
        initializeGame()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        animalType = AnimalType.DOBERMAN
        initializeGame()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        animalType = AnimalType.DOBERMAN
        initializeGame()
    }

    constructor(context: Context, animalType: AnimalType) : super(context) {
        this.animalType = animalType
        initializeGame()
    }

    // 在类的顶部添加这些常量，方便您微调背景参数
    companion object {
        // 背景参数 - 您可以调整这些值来微调背景位置和大小
        var initialBgX1 = 0f  // 初始背景1的X位置
        var initialBgX2Offset = 1f  // 初始背景2相对于背景1的偏移量
        var backgroundSpeed = 4f  // 背景滚动速度
        var backgroundScaleX = 0.35f  // 背景X轴缩放比例
        var backgroundScaleY = 0.35f  // 背景Y轴缩放比例
        var backgroundOffsetY = 0f  // 背景Y轴偏移量
    }
    // Game states
    enum class GameState { READY, PREPARING, RUNNING_IN, RUNNING, OVER }

    // Game phases
    enum class GamePhase { EXERCISE, REST }

    // Difficulty levels with custom spawn times and probabilities
    enum class Difficulty(
        val exerciseTime: Int,
        val restTime: Int,
        val minSpawnTime: Float,  // 最小生成間隔（秒）
        val maxSpawnTime: Float   // 最大生成間隔（秒）
    ) {
        EASY(5, 5, 2.0f, 4.0f),
        NORMAL(10, 5, 1.5f, 3.0f),
        HARD(15, 5, 1.0f, 2.0f)
    }

    // Game objects
    private data class GameObject(var x: Float, var y: Float, val width: Float, val height: Float, val type: GameObjectType)
    enum class GameObjectType { SMALL_ROCK, BIG_ROCK, COIN, BUSH, BUSH_SMALL }

    // Game properties
    private var gameState = GameState.READY
    private var currentPhase = GamePhase.EXERCISE
    private var currentDifficulty = Difficulty.NORMAL
    private var score = 0
    private var distance = 0
    private var distanceAccumulator = 0f  // 添加這個變量來累積小數部分的距離
    private var coins = 0
    private var health = 3
    private var totalGameTime = 0L
    private var phaseTimer = 0L
    private var prepareTimer = 3000L
    private var runningInTimer = 0L  // 新增：動物跑入畫面的計時器
    private var isExercising = false
    private var exerciseForce = 0f
    private var gameTimeLeft = 60000L // 1 minute in milliseconds
    private var invincibleTimeRemaining = 0L // Renamed from invincibleTime to avoid conflict
    private var isInvincible = false
    private var blinkCounter = 0
    private var obstacleSpawnTimer = 0f
    private var coinSpawnTimer = 0f  // 新增金幣生成計時器
    private var bushSpawnTimer = 0f  // 新增草叢生成計時器
    private var lastObstacleType = GameObjectType.SMALL_ROCK  // 追蹤最後生成的障礙物類型

    // Animal animation properties
    private var animalY = 0f
    private var animalX = -120f  // 初始位置在畫面外
    private val animalWidth = 120f
    private val animalHeight = 120f
    private var currentFrame = 0
    private var frameCounter = 0
    private lateinit var animalWalkFrames: Array<Bitmap>  // 存储动物的行走动画帧
    private lateinit var animalIdleFrames: Array<Bitmap>  // 存储动物的待机动画帧
    private val finalAnimalX = 100f  // 動物最終的X位置

    // Background properties - 使用您提供的参数
    private lateinit var backgroundBitmap: Bitmap
    private var backgroundX1 = 1f
    private var backgroundX2 = 1f

    // Game objects
    private val gameObjects = mutableListOf<GameObject>()
    private var groundY = 0f

    // Drawing
    private val paint = Paint()
    private lateinit var rockSmallBitmap: Bitmap
    private lateinit var rockBigBitmap: Bitmap
    private lateinit var coinBitmap: Bitmap
    private lateinit var bushBitmap: Bitmap  // 草叢圖片
    private lateinit var bushSmallBitmap: Bitmap  // 小草叢圖片

    // Sound
    private lateinit var soundPool: SoundPool
    private var jumpSoundId = 0
    private var coinSoundId = 0
    private var hitSoundId = 0
    // Add the new sound IDs
    private var forceSoundId = 0
    private var restSoundId = 0

    // Game loop
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 16L // ~60 FPS
    private val gameLoop = object : Runnable {
        override fun run() {
            updateGame()
            invalidate()
            handler.postDelayed(this, updateInterval)
        }
    }

    // Constants
    private val gameSpeed = 8f
    private val gravity = 0.5f
    private val smoothingFactor = 0.1f
    private val maxExerciseForce = 100f
    private val forceIncreaseRate = 5f
    private val forceDecayRate = 2f
    private val verticalSpeed = 10f
    private val jumpHeight = 150f
    private val animationSpeed = 5
    private val invincibleTime = 1500L // 1.5 seconds of invincibility
    private val blinkInterval = 150L // Blink every 150ms
    private val coinScore = 50 // Points per coin
    private val coinMinSpawnTime = 1.0f // 金幣最小生成間隔（秒）
    private val coinMaxSpawnTime = 2.5f // 金幣最大生成間隔（秒）
    private val runningInTime = 1500L // 動物跑入畫面的時間（毫秒）
    private val bushMinSpawnTime = 0.1f // 草叢最小生成間隔（秒）- 從0.2f減少到0.1f
    private val bushMaxSpawnTime = 0.3f // 草叢最大生成間隔（秒）- 從0.5f減少到0.3f

    // 金幣高度常數
    private val coinAirHeight = 200f // 空中金幣的高度（相對於地面）
    private val coinGroundHeight = 50f // 地面金幣的高度（相對於地面）

    // Callback
    private var gameOverListener: (() -> Unit)? = null

    private val tag = "GameView"

    // 在類的常量部分添加一個新的常量，用於定義金幣和障礙物之間的最小安全距離
    private val minCoinObstacleDistance = 150f  // 金幣和障礙物之間的最小安全距離
    private val minObstacleDistance = 150f  // 障礙物之間的最小安全距離
    private val bushSpacing = 120f  // 草叢之間的標準間距 - 從200f減少到120f

    private fun initializeGame() {
        // 在日誌中輸出使用的動物類型，以便調試
        android.util.Log.d("GameView", "Using animal type: $animalType")

        // 加载动物的sprite sheet并提取帧
        animalWalkFrames = animalType.extractFrames(context)
        animalIdleFrames = animalType.extractFrames(context, true)

        // 加载背景图
        backgroundBitmap = BitmapFactory.decodeResource(resources, animalType.backgroundResId)

        // 初始化背景位置 - 使用可调整的参数
        backgroundX1 = initialBgX1
        backgroundX2 = backgroundX1 + backgroundBitmap.width * backgroundScaleX * initialBgX2Offset

        // 加載其他圖片
        rockSmallBitmap = BitmapFactory.decodeResource(resources, R.drawable.small_rock)
        rockBigBitmap = BitmapFactory.decodeResource(resources, R.drawable.big_rock)
        coinBitmap = BitmapFactory.decodeResource(resources, R.drawable.coin)
        bushBitmap = BitmapFactory.decodeResource(resources, R.drawable.bush)  // 加載草叢圖片
        bushSmallBitmap = BitmapFactory.decodeResource(resources, R.drawable.bush_small)  // 加載小草叢圖片

        // Initialize sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(3).build()
        jumpSoundId = soundPool.load(context, R.raw.jump, 1)
        coinSoundId = soundPool.load(context, R.raw.eat_coin, 1)
        hitSoundId = soundPool.load(context, R.raw.hit, 1)
        // Add these two lines to load the new sounds
        forceSoundId = soundPool.load(context, R.raw.force, 1)
        restSoundId = soundPool.load(context, R.raw.rest, 1)

        // Set up paint
        paint.isAntiAlias = true

        // Reset game state
        resetGame()
    }

    // 修改resetGame方法中的背景重置部分
    fun resetGame() {
        gameState = GameState.READY
        currentPhase = GamePhase.EXERCISE
        currentDifficulty = Difficulty.NORMAL
        score = 0
        distance = 0
        distanceAccumulator = 0f  // 重置累積器
        coins = 0
        health = 3
        totalGameTime = 0L
        gameTimeLeft = 60000L
        phaseTimer = 0L
        prepareTimer = 3000L
        runningInTimer = runningInTime
        isExercising = false
        exerciseForce = 0f
        isInvincible = false
        invincibleTimeRemaining = 0L
        blinkCounter = 0
        obstacleSpawnTimer = 0f
        coinSpawnTimer = 0f
        bushSpawnTimer = 0f  // 重置草叢生成計時器
        lastObstacleType = GameObjectType.SMALL_ROCK  // 重置最後生成的障礙物類型
        gameObjects.clear()

        // Reset animal position if view is already sized
        if (width > 0 && height > 0) {
            groundY = height * 0.8f
            animalY = groundY - animalHeight
            animalX = -animalWidth  // 將動物放在畫面外
        }

        // 重置背景位置，确保在游戏重置时背景位置一致
        backgroundX1 = initialBgX1
        backgroundX2 = backgroundX1 + backgroundBitmap.width * backgroundScaleX * initialBgX2Offset

        // Stop any running game loop
        handler.removeCallbacks(gameLoop)

        // Redraw the view
        invalidate()
    }

    // 修改startGame方法中的背景重置部分
    fun startGame(difficulty: Difficulty = Difficulty.NORMAL) {
        // Only start if in READY state
        if (gameState == GameState.READY) {
            gameState = GameState.PREPARING
            currentDifficulty = difficulty
            prepareTimer = 3000L
            runningInTimer = runningInTime
            score = 0
            distance = 0
            distanceAccumulator = 0f  // 重置累積器
            coins = 0
            health = 3
            totalGameTime = 0L
            gameTimeLeft = 60000L
            phaseTimer = currentDifficulty.exerciseTime * 1000L
            currentPhase = GamePhase.EXERCISE
            isInvincible = false
            invincibleTimeRemaining = 0L
            obstacleSpawnTimer = 0f
            coinSpawnTimer = 0f
            bushSpawnTimer = 0f  // 重置草叢生成計時器
            lastObstacleType = GameObjectType.SMALL_ROCK  // 重置最後生成的障礙物類型
            gameObjects.clear()

            // 將動物放在畫面外
            animalX = -animalWidth

            // 确保游戏开始时背景位置一致
            backgroundX1 = initialBgX1
            backgroundX2 = backgroundX1 + backgroundBitmap.width * backgroundScaleX * initialBgX2Offset

            // Start game loop
            handler.removeCallbacks(gameLoop) // Remove any existing callbacks first
            handler.post(gameLoop)
        }
    }

    private fun updateGame() {
        when (gameState) {
            GameState.PREPARING -> {
                prepareTimer -= updateInterval
                if (prepareTimer <= 0) {
                    gameState = GameState.RUNNING_IN
                }
                // 在準備階段也更新背景
                updateBackground()
            }
            GameState.RUNNING_IN -> {
                // 更新動物跑入畫面的動畫
                runningInTimer -= updateInterval

                // 計算動物的X位置，從畫面外跑到最終位置
                val progress = 1f - (runningInTimer / runningInTime.toFloat())
                animalX = -animalWidth + (finalAnimalX + animalWidth) * progress

                // 更新動物動畫
                updateAnimation()

                // 更新背景
                updateBackground()

                // 當動物完全跑入畫面後，切換到正常遊戲狀態
                if (runningInTimer <= 0) {
                    gameState = GameState.RUNNING
                    animalX = finalAnimalX  // 確保動物位置正確
                    // 在遊戲開始時播放運動階段聲音
                    soundPool.play(forceSoundId, 1f, 1f, 1, 0, 1f)
                }
            }
            GameState.RUNNING -> {
                totalGameTime += updateInterval
                gameTimeLeft -= updateInterval
                phaseTimer -= updateInterval

                // Update phase
                if (phaseTimer <= 0) {
                    if (currentPhase == GamePhase.EXERCISE) {
                        currentPhase = GamePhase.REST
                        phaseTimer = (currentDifficulty.restTime * 1000).toLong()
                        // Play the rest sound when changing to REST phase
                        soundPool.play(restSoundId, 1f, 1f, 1, 0, 1f)
                    } else {
                        currentPhase = GamePhase.EXERCISE
                        phaseTimer = (currentDifficulty.exerciseTime * 1000).toLong()
                        // Play the force sound when changing to EXERCISE phase
                        soundPool.play(forceSoundId, 1f, 1f, 1, 0, 1f)
                    }
                }

                // Always update distance (scene keeps moving)
                distanceAccumulator += (gameSpeed * updateInterval / 1000)
                if (distanceAccumulator >= 1f) {
                    val increment = distanceAccumulator.toInt()
                    distance += increment
                    distanceAccumulator -= increment
                }

                // Update invincibility
                if (isInvincible) {
                    invincibleTimeRemaining -= updateInterval
                    // Fix: Convert to Long before division to avoid rem operator issue
                    blinkCounter = ((blinkCounter + updateInterval.toInt()) / (blinkInterval.toInt() / 2) * (blinkInterval.toInt() / 2)).toInt()

                    if (invincibleTimeRemaining <= 0) {
                        isInvincible = false
                    }
                }

                updateAnimalPosition(updateInterval / 1000f)
                updateGameObjects(updateInterval / 1000f)
                updateBackground()
                checkCollisions()
                updateAnimation()

                // Check if game time is up
                if (gameTimeLeft <= 0) {
                    gameState = GameState.OVER
                    handler.removeCallbacks(gameLoop)
                    gameOverListener?.invoke()
                }

                // Update score: score is just the distance
                score = distance
            }
            else -> {}
        }
    }

    // 修改updateAnimation方法，讓RUNNING_IN狀態也始終使用walk動畫
    private fun updateAnimation() {
        frameCounter++
        if (frameCounter >= animationSpeed) {
            frameCounter = 0
            // 修改：在遊戲運行狀態和跑入畫面狀態下，始終使用walk動畫
            val maxFrames = if (gameState == GameState.RUNNING || gameState == GameState.RUNNING_IN) {
                animalWalkFrames.size
            } else {
                // 在其他狀態下，根據isExercising決定
                if (isExercising) animalWalkFrames.size else animalIdleFrames.size
            }
            currentFrame = (currentFrame + 1) % maxFrames
        }
    }

    // 修改updateBackground方法
    private fun updateBackground() {
        // 移动背景
        backgroundX1 -= backgroundSpeed
        backgroundX2 -= backgroundSpeed

        // 计算缩放后的背景宽度
        val scaledWidth = backgroundBitmap.width * backgroundScaleX

        // 当第一张背景完全移出屏幕时，将其放到第二张背景后面
        if (backgroundX1 + scaledWidth <= 0) {
            backgroundX1 = backgroundX2 + scaledWidth
        }

        // 当第二张背景完全移出屏幕时，将其放到第一张背景后面
        if (backgroundX2 + scaledWidth <= 0) {
            backgroundX2 = backgroundX1 + scaledWidth
        }
    }

    private fun updateGameObjects(deltaTime: Float) {
        // Always move existing objects (scene keeps moving)
        gameObjects.forEach { it.x -= gameSpeed }
        gameObjects.removeAll { it.x + it.width < 0 }

        // Update spawn timers
        obstacleSpawnTimer -= deltaTime
        coinSpawnTimer -= deltaTime
        bushSpawnTimer -= deltaTime

        // 根據當前階段生成不同的物品
        if (currentPhase == GamePhase.EXERCISE) {
            // 運動階段：生成障礙物和金幣
            if (obstacleSpawnTimer <= 0 || gameObjects.isEmpty()) {
                // 交替生成石頭和草叢
                if (lastObstacleType == GameObjectType.BUSH || lastObstacleType == GameObjectType.BUSH_SMALL) {
                    addNewObstacle()
                    lastObstacleType = GameObjectType.SMALL_ROCK  // 更新為石頭類型
                } else {
                    addNewBush()
                    lastObstacleType = GameObjectType.BUSH  // 更新為草叢類型
                }

                // Reset spawn timer with random value based on difficulty
                obstacleSpawnTimer = (currentDifficulty.minSpawnTime * 0.5f) +
                        (Math.random() * (currentDifficulty.maxSpawnTime - currentDifficulty.minSpawnTime) * 0.5f).toFloat()
            }

            // 金幣生成（運動階段）
            if (coinSpawnTimer <= 0) {
                addNewCoin(true) // 運動階段可以生成空中金幣
                coinSpawnTimer = coinMinSpawnTime +
                        (Math.random() * (coinMaxSpawnTime - coinMinSpawnTime)).toFloat()
            }

            // 額外的草叢生成（只在運動階段）- 保留這個以增加草叢密度
            if (bushSpawnTimer <= 0) {
                // 檢查最後一個障礙物是否為石頭
                if (lastObstacleType == GameObjectType.SMALL_ROCK || lastObstacleType == GameObjectType.BIG_ROCK) {
                    // 檢查是否有足夠的空間生成草叢
                    val lastObstacleX = findLastObstacleX()
                    if (width.toFloat() - lastObstacleX > bushSpacing * 0.8f) { // 減少所需空間
                        addNewBush()
                    }
                } else {
                    // 即使最後一個障礙物是草叢，也有30%機率再生成更多草叢
                    if (Math.random() < 0.3) {
                        val lastObstacleX = findLastObstacleX()
                        if (width.toFloat() - lastObstacleX > bushSpacing * 1.5f) {
                            addNewBush()
                        }
                    }
                }

                bushSpawnTimer = bushMinSpawnTime +
                        (Math.random() * (bushMaxSpawnTime - bushMinSpawnTime)).toFloat()
            }
        } else {
            // 休息階段：只生成金幣，且只在地面上
            if (coinSpawnTimer <= 0) {
                addNewCoin(false) // 休息階段只生成地面金幣
                coinSpawnTimer = coinMinSpawnTime +
                        (Math.random() * (coinMaxSpawnTime - coinMinSpawnTime)).toFloat()
            }
        }
    }

    // 找到最後一個障礙物的X座標
    private fun findLastObstacleX(): Float {
        var lastX = -1000f
        gameObjects.forEach { obj ->
            if (obj.type == GameObjectType.SMALL_ROCK || obj.type == GameObjectType.BIG_ROCK ||
                obj.type == GameObjectType.BUSH || obj.type == GameObjectType.BUSH_SMALL
            ) {
                if (obj.x > lastX) {
                    lastX = obj.x
                }
            }
        }
        return lastX
    }

    // 新增障礙物生成方法
    private fun addNewObstacle() {
        val random = Math.random()
        val obstacleType = if (random < 0.5) GameObjectType.SMALL_ROCK else GameObjectType.BIG_ROCK

        // 檢查是否與現有障礙物太靠近
        val tooCloseToObstacle = gameObjects.any { obj ->
            val isObstacle = obj.type == GameObjectType.SMALL_ROCK ||
                    obj.type == GameObjectType.BIG_ROCK ||
                    obj.type == GameObjectType.BUSH ||
                    obj.type == GameObjectType.BUSH_SMALL

            if (isObstacle) {
                // 計算與障礙物之間的水平距離
                val distance = Math.abs(width.toFloat() - obj.x)
                // 如果距離小於最小安全距離，則認為太靠近
                distance < minObstacleDistance
            } else {
                false
            }
        }

        // 只有當不太靠近其他障礙物時才生成
        if (!tooCloseToObstacle) {
            val newObstacle = when (obstacleType) {
                GameObjectType.SMALL_ROCK -> GameObject(
                    width.toFloat(),
                    groundY - 80,  // 調整小石頭的 Y 位置，讓它與其他障礙物對齊
                    80f,  // 寬度
                    80f,  // 高度
                    GameObjectType.SMALL_ROCK
                )
                else -> GameObject(
                    width.toFloat(),
                    groundY - 100,  // 大石頭的 Y 位置保持不變
                    100f,  // 寬度
                    100f,  // 高度
                    GameObjectType.BIG_ROCK
                )
            }
            gameObjects.add(newObstacle)

            // 有50%機率在不同高度再生成一個石頭（如果空間允許）
            if (Math.random() < 0.5) {
                // 檢查新位置是否與現有障礙物太靠近
                val offsetX = if (obstacleType == GameObjectType.SMALL_ROCK) 100f else 120f  // 水平偏移
                val newX = width.toFloat() + offsetX

                val tooCloseToOtherObstacle = gameObjects.any { obj ->
                    if (obj.type == GameObjectType.SMALL_ROCK || obj.type == GameObjectType.BIG_ROCK ||
                        obj.type == GameObjectType.BUSH || obj.type == GameObjectType.BUSH_SMALL
                    ) {
                        val distance = Math.abs(newX - obj.x)
                        distance < minObstacleDistance * 0.5f  // 使用較小的安全距離
                    } else {
                        false
                    }
                }

                if (!tooCloseToOtherObstacle) {
                    // 使用另一種石頭類型
                    val anotherObstacleType = if (obstacleType == GameObjectType.SMALL_ROCK) GameObjectType.BIG_ROCK else GameObjectType.SMALL_ROCK
                    val anotherObstacle = when (anotherObstacleType) {
                        GameObjectType.SMALL_ROCK -> GameObject(
                            newX,
                            groundY - 80,  // 調整小石頭的 Y 位置，讓它與其他障礙物對齊
                            80f,  // 寬度
                            80f,  // 高度
                            GameObjectType.SMALL_ROCK
                        )
                        else -> GameObject(
                            newX,
                            groundY - 100,  // 大石頭的 Y 位置保持不變
                            100f,  // 寬度
                            100f,  // 高度
                            GameObjectType.BIG_ROCK
                        )
                    }
                    gameObjects.add(anotherObstacle)
                }
            }
        }
    }

    // 新增草叢生成方法
    private fun addNewBush() {
        // 隨機決定使用大草叢還是小草叢
        val useBigBush = Math.random() < 0.5
        val bushType = if (useBigBush) GameObjectType.BUSH else GameObjectType.BUSH_SMALL

        // 根據草叢類型設置尺寸
        val bushWidth = if (useBigBush) 80f else 60f
        val bushHeight = if (useBigBush) 70f else 50f

        // 檢查是否與現有障礙物太靠近
        val tooCloseToObstacle = gameObjects.any { obj ->
            val isObstacle = obj.type == GameObjectType.SMALL_ROCK ||
                    obj.type == GameObjectType.BIG_ROCK ||
                    obj.type == GameObjectType.BUSH ||
                    obj.type == GameObjectType.BUSH_SMALL

            if (isObstacle) {
                // 計算與障礙物之間的水平距離
                val distance = Math.abs(width.toFloat() - obj.x)
                // 如果距離小於最小安全距離，則認為太靠近
                distance < minObstacleDistance * 0.7f // 減少安全距離以允許更密集的草叢
            } else {
                false
            }
        }

        // 只有當不太靠近其他障礙物時才生成
        if (!tooCloseToObstacle) {
            val newBush = GameObject(
                width.toFloat(),
                groundY - bushHeight,  // 草叢放在地面上
                bushWidth,
                bushHeight,
                bushType
            )
            gameObjects.add(newBush)

            // 計算草叢之間的間距 - 使用較小的間距
            val spacing1 = bushSpacing + (Math.random() * 30).toFloat()
            val spacing2 = bushSpacing + (Math.random() * 30).toFloat()
            val spacing3 = bushSpacing + (Math.random() * 30).toFloat()

            // 生成第二個草叢
            val secondBushX = width.toFloat() + spacing1
            val secondBushType = if (useBigBush) GameObjectType.BUSH_SMALL else GameObjectType.BUSH
            val secondBushWidth = if (secondBushType == GameObjectType.BUSH) 80f else 60f
            val secondBushHeight = if (secondBushType == GameObjectType.BUSH) 70f else 50f

            val secondBush = GameObject(
                secondBushX,
                groundY - secondBushHeight,
                secondBushWidth,
                secondBushHeight,
                secondBushType
            )
            gameObjects.add(secondBush)

            // 增加到80%機率生成第三個草叢
            if (Math.random() < 0.8) {
                val thirdBushX = secondBushX + spacing2
                val thirdBushType = if (Math.random() < 0.5) GameObjectType.BUSH else GameObjectType.BUSH_SMALL
                val thirdBushWidth = if (thirdBushType == GameObjectType.BUSH) 80f else 60f
                val thirdBushHeight = if (thirdBushType == GameObjectType.BUSH) 70f else 50f

                val thirdBush = GameObject(
                    thirdBushX,
                    groundY - thirdBushHeight,
                    thirdBushWidth,
                    thirdBushHeight,
                    thirdBushType
                )
                gameObjects.add(thirdBush)

                // 新增：60%機率生成第四個草叢
                if (Math.random() < 0.6) {
                    val fourthBushX = thirdBushX + spacing3
                    val fourthBushType = if (Math.random() < 0.5) GameObjectType.BUSH else GameObjectType.BUSH_SMALL
                    val fourthBushWidth = if (fourthBushType == GameObjectType.BUSH) 80f else 60f
                    val fourthBushHeight = if (fourthBushType == GameObjectType.BUSH) 70f else 50f

                    val fourthBush = GameObject(
                        fourthBushX,
                        groundY - fourthBushHeight,
                        fourthBushWidth,
                        fourthBushHeight,
                        fourthBushType
                    )
                    gameObjects.add(fourthBush)
                }
            }
        }
    }

    // 修改 addNewCoin 方法，檢查與現有障礙物的距離
    private fun addNewCoin(canBeInAir: Boolean) {
        // 根據階段決定金幣高度
        val coinHeight = if (canBeInAir) {
            groundY - coinAirHeight  // 訓練階段：空中金幣
        } else {
            groundY - coinGroundHeight  // 休息階段：地面金幣
        }

        // 計算新金幣的位置
        val coinX = width.toFloat()
        val coinWidth = 50f

        // 檢查是否與現有障礙物太靠近
        val tooCloseToObstacle = gameObjects.any { obj ->
            if (obj.type == GameObjectType.SMALL_ROCK || obj.type == GameObjectType.BIG_ROCK ||
                obj.type == GameObjectType.BUSH || obj.type == GameObjectType.BUSH_SMALL
            ) {
                // 計算金幣和障礙物之間的水平距離
                val distance = Math.abs(coinX - obj.x)
                // 如果距離小於最小安全距離，則認為太靠近
                distance < minCoinObstacleDistance
            } else {
                false
            }
        }

        // 只有當不太靠近障礙物時才生成金幣
        if (!tooCloseToObstacle) {
            val newCoin = GameObject(
                coinX,
                coinHeight,
                coinWidth,
                50f,  // 高度
                GameObjectType.COIN
            )
            gameObjects.add(newCoin)
        }
    }

    private fun checkCollisions() {
        if (isInvincible) return // Skip collision check if invincible

        val animalRect = RectF(animalX, animalY, animalX + animalWidth, animalY + animalHeight)

        val iterator = gameObjects.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            val objRect = RectF(obj.x, obj.y, obj.x + obj.width, obj.y + obj.height)

            if (RectF.intersects(animalRect, objRect)) {
                when (obj.type) {
                    GameObjectType.SMALL_ROCK, GameObjectType.BIG_ROCK, GameObjectType.BUSH, GameObjectType.BUSH_SMALL -> {
                        health--
                        soundPool.play(hitSoundId, 1f, 1f, 1, 0, 1f)
                        iterator.remove()

                        // Set invincibility
                        isInvincible = true
                        invincibleTimeRemaining = invincibleTime

                        if (health <= 0) {
                            gameState = GameState.OVER
                            handler.removeCallbacks(gameLoop)
                            gameOverListener?.invoke()
                        }
                    }
                    GameObjectType.COIN -> {
                        // 收集金幣時增加里程，隨機增加50或100
                        val bonusMileage = if (Math.random() < 0.5) 50 else 100
                        distance += bonusMileage
                        coins++
                        soundPool.play(coinSoundId, 1f, 1f, 1, 0, 1f)
                        iterator.remove()
                    }
                }
            }
        }
    }

    // 修改onDraw方法中繪製動物的部分，讓RUNNING_IN狀態也始終使用walk動畫
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制循环背景 - 使用可调整的缩放参数
        val bgWidth = backgroundBitmap.width * backgroundScaleX
        val bgHeight = backgroundBitmap.height * backgroundScaleY

        // 计算背景的目标矩形
        val bgRect1 = RectF(
            backgroundX1,
            backgroundOffsetY,
            backgroundX1 + bgWidth,
            backgroundOffsetY + bgHeight
        )

        val bgRect2 = RectF(
            backgroundX2,
            backgroundOffsetY,
            backgroundX2 + bgWidth,
            backgroundOffsetY + bgHeight
        )

        canvas.drawBitmap(backgroundBitmap, null, bgRect1, paint)
        canvas.drawBitmap(backgroundBitmap, null, bgRect2, paint)

        // 绘制地面
        paint.color = Color.GRAY
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, paint)

        // 只在RUNNING状态下绘制游戏对象
        if (gameState == GameState.RUNNING) {
            // 繪製所有遊戲物件
            gameObjects.forEach { obj ->
                val bitmap = when (obj.type) {
                    GameObjectType.SMALL_ROCK -> rockSmallBitmap
                    GameObjectType.BIG_ROCK -> rockBigBitmap
                    GameObjectType.COIN -> coinBitmap
                    GameObjectType.BUSH -> bushBitmap
                    GameObjectType.BUSH_SMALL -> bushSmallBitmap
                }
                canvas.drawBitmap(bitmap, null, RectF(obj.x, obj.y, obj.x + obj.width, obj.y + obj.height), paint)
            }
        }

        // 只在RUNNING_IN和RUNNING状态下绘制动物
        if (gameState == GameState.RUNNING_IN || gameState == GameState.RUNNING) {
            // 绘制动物 (带闪烁效果如果无敌)
            if (!isInvincible || blinkCounter < (blinkInterval / 2).toInt()) {
                // 修改：在遊戲運行狀態和跑入畫面狀態下，始終使用walk動畫
                val animalFrames = if (gameState == GameState.RUNNING || gameState == GameState.RUNNING_IN) {
                    animalWalkFrames
                } else {
                    // 在其他狀態下，根據isExercising決定
                    if (isExercising) animalWalkFrames else animalIdleFrames
                }
                // 確保當前幀索引不超出範圍
                val frameIndex = currentFrame % animalFrames.size
                val animalBitmap = animalFrames[frameIndex]
                canvas.drawBitmap(animalBitmap, null, RectF(animalX, animalY, animalX + animalWidth, animalY + animalHeight), paint)
            }
        }

        // 绘制游戏信息
        drawGameInfo(canvas)

        // 绘制阶段计时器
        if (gameState == GameState.RUNNING) {
            drawPhaseTimer(canvas)
        }
    }

    private fun drawPhaseTimer(canvas: Canvas) {
        val phaseSecondsLeft = (phaseTimer / 1000).toInt()  // 確保轉換為 Int
        val phaseText = if (currentPhase == GamePhase.EXERCISE) "運動" else "休息"

        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        paint.color = if (currentPhase == GamePhase.EXERCISE) Color.rgb(33, 150, 243) else Color.rgb(76, 175, 80)

        val centerX = width / 2f
        val centerY = height / 3f

        // Draw semi-transparent background
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(100, 0, 0, 0)
        canvas.drawRect(centerX - 150, centerY - 100, centerX + 150, centerY + 50, paint)

        // Draw text
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawText(phaseText, centerX, centerY - 30, paint)
        canvas.drawText(phaseSecondsLeft.toString(), centerX, centerY + 40, paint)  // 使用 toString()
    }

    // 修改onSizeChanged方法中的背景重置部分
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        groundY = h * 0.8f
        animalY = groundY - animalHeight

        // 在尺寸变化时保持背景位置一致
        backgroundX1 = initialBgX1
        backgroundX2 = backgroundX1 + backgroundBitmap.width * backgroundScaleX * initialBgX2Offset
    }

    fun setExercising(exercising: Boolean) {
        isExercising = exercising
        if (isExercising && gameState == GameState.RUNNING) {
            soundPool.play(jumpSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    fun isGameOver(): Boolean = gameState == GameState.OVER

    fun getScore(): Int = score

    fun setGameOverListener(listener: () -> Unit) {
        gameOverListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(gameLoop)
        soundPool.release()

        // 释放位图资源
        for (bitmap in animalWalkFrames) {
            bitmap.recycle()
        }
        for (bitmap in animalIdleFrames) {
            bitmap.recycle()
        }
        backgroundBitmap.recycle()
        rockSmallBitmap.recycle()
        rockBigBitmap.recycle()
        coinBitmap.recycle()
        bushBitmap.recycle()  // 釋放草叢圖片資源
        bushSmallBitmap.recycle()  // 釋放小草叢圖片資源
    }

    private fun updateAnimalPosition(deltaTime: Float) {
        if (isExercising) {
            exerciseForce = min(maxExerciseForce, exerciseForce + forceIncreaseRate)
        } else {
            exerciseForce = max(0f, exerciseForce - forceDecayRate)
        }

        val forceRatio = exerciseForce / maxExerciseForce
        val targetY = groundY - animalHeight - jumpHeight * forceRatio

        // Use lerp for smoother movement
        animalY = lerp(animalY, targetY, verticalSpeed * deltaTime)

        // Ensure the animal doesn't go below the ground
        if (animalY > groundY - animalHeight) {
            animalY = groundY - animalHeight
        }
    }

    // Helper function for linear interpolation
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        val clampedFraction = if (fraction < 0f) 0f else if (fraction > 1f) 1f else fraction
        return start + (end - start) * clampedFraction
    }

    private fun drawGameInfo(canvas: Canvas) {
        paint.color = Color.WHITE  // 改为白色以便在背景上更清晰
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL

        // 添加文字阴影以增强可读性
        paint.setShadowLayer(3f, 2f, 2f, Color.BLACK)

        // Draw game stats
        canvas.drawText("分數: $score", 20f, 50f, paint)
        canvas.drawText("里程: $distance", 20f, 100f, paint)
        canvas.drawText("生命: $health", 20f, 150f, paint)
        canvas.drawText("金幣: $coins", 20f, 200f, paint)  // 顯示金幣數量

        // Draw timers
        if (gameState == GameState.RUNNING) {
            // Draw game time left
            val minutesLeft = (gameTimeLeft / 60000).toInt()
            val secondsLeft = ((gameTimeLeft % 60000) / 1000).toInt()
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("剩餘時間: ${String.format(Locale.getDefault(), "%02d:%02d", minutesLeft, secondsLeft)}",
                width - 20f, 50f, paint)
        }

        // 移除阴影效果，以免影响其他绘制
        paint.clearShadowLayer()

        // Draw game state messages
        paint.textAlign = Paint.Align.CENTER
        val centerX = width / 2f
        val centerY = height / 2f

        when (gameState) {
            GameState.READY -> {
                // 添加半透明背景以增强可读性
                paint.color = Color.argb(150, 0, 0, 0)
                canvas.drawRect(centerX - 300, centerY - 50, centerX + 300, centerY + 50, paint)

                paint.color = Color.WHITE
                paint.textSize = 60f
                canvas.drawText("按開始遊戲按鈕開始", centerX, centerY, paint)
            }
            GameState.PREPARING -> {
                // 添加半透明背景
                paint.color = Color.argb(150, 0, 0, 0)
                canvas.drawRect(centerX - 200, centerY - 50, centerX + 200, centerY + 100, paint)

                paint.color = Color.WHITE
                paint.textSize = 60f
                canvas.drawText("準備開始！", centerX, centerY, paint)
                paint.textSize = 40f
                val prepareSecondsLeft = (prepareTimer / 1000).toInt() + 1
                canvas.drawText("$prepareSecondsLeft 秒", centerX, centerY + 70f, paint)
            }
            GameState.RUNNING_IN -> {
                // 在動物跑入畫面時不顯示特殊訊息
            }
            GameState.OVER -> {
                // 添加半透明背景
                paint.color = Color.argb(150, 0, 0, 0)
                canvas.drawRect(centerX - 200, centerY - 50, centerX + 200, centerY + 150, paint)

                paint.color = Color.WHITE
                paint.textSize = 60f
                canvas.drawText("遊戲結束", centerX, centerY, paint)
                paint.textSize = 40f
                canvas.drawText("最終分數: $score", centerX, centerY + 70f, paint)
                canvas.drawText("收集金幣: $coins", centerX, centerY + 130f, paint)
            }
            else -> {}
        }
    }
}
