package com.example.myapplication.game

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color

class GameActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var startButton: Button
    private lateinit var exerciseButton: Button
    private lateinit var difficultyGroup: RadioGroup
    private lateinit var selectedAnimal: AnimalType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedAnimal = intent.getStringExtra("SELECTED_ANIMAL")?.let {
            try {
                AnimalType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                AnimalType.DOBERMAN
            }
        } ?: AnimalType.DOBERMAN

        android.util.Log.d("GameActivity", "Selected animal: $selectedAnimal")

        val layout = createMainLayout()
        setContentView(layout)

        gameView.setGameOverListener {
            runOnUiThread { showGameOverDialog() }
        }
    }

    private fun createMainLayout(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
        }

        gameView = GameView(this, selectedAnimal)
        layout.addView(gameView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0
        ).apply {
            weight = 1f
            bottomMargin = 16
        })

        difficultyGroup = createDifficultyGroup()
        layout.addView(difficultyGroup, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        startButton = Button(this).apply {
            text = "開始遊戲"
            setBackgroundColor(Color.rgb(76, 175, 80))
            setTextColor(Color.WHITE)
            setOnClickListener { startGame() }
        }
        layout.addView(startButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        exerciseButton = Button(this).apply {
            text = "運動"
            visibility = View.GONE
            setBackgroundColor(Color.rgb(33, 150, 243))
            setTextColor(Color.WHITE)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { gameView.setExercising(true); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { gameView.setExercising(false); true }
                    else -> false
                }
            }
        }
        layout.addView(exerciseButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        return layout
    }

    private fun createDifficultyGroup(): RadioGroup {
        val group = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        val difficulties = arrayOf("簡單", "普通", "困難")
        difficulties.forEachIndexed { index, text ->
            val radioButton = RadioButton(this).apply {
                this.text = text
                id = index
                setTextColor(Color.BLACK)
            }
            group.addView(radioButton)
            if (index == 1) radioButton.isChecked = true
        }
        return group
    }

    private fun startGame() {
        val difficulty = when (difficultyGroup.checkedRadioButtonId) {
            0 -> GameView.Difficulty.EASY
            1 -> GameView.Difficulty.NORMAL
            2 -> GameView.Difficulty.HARD
            else -> GameView.Difficulty.NORMAL
        }

        startButton.visibility = View.GONE
        difficultyGroup.visibility = View.GONE
        exerciseButton.visibility = View.VISIBLE

        gameView.startGame(difficulty)
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("遊戲結束")
            .setMessage("你的分數是: ${gameView.getScore()}")
            .setPositiveButton("再玩一次") { _, _ -> resetGame() }
            .setNegativeButton("退出") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun resetGame() {
        gameView.resetGame()
        startButton.visibility = View.VISIBLE
        difficultyGroup.visibility = View.VISIBLE
        exerciseButton.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (gameView.isGameOver()) {
            super.onBackPressed()
        } else {
            AlertDialog.Builder(this)
                .setTitle("退出遊戲")
                .setMessage("確定要退出遊戲嗎？")
                .setPositiveButton("是") { _, _ -> finish() }
                .setNegativeButton("否", null)
                .show()
        }
    }
}
