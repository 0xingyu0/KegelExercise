package com.example.myapplication.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.game.AnimalType

class AnimalSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animal_selection)

        val recyclerView = findViewById<RecyclerView>(R.id.animalRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AnimalAdapter(AnimalType.values()) { selectedAnimal ->
            // 當選擇動物時，啟動主遊戲並傳遞選擇的動物
            val intent = Intent(this, com.example.myapplication.game.GameActivity::class.java).apply {
                putExtra("SELECTED_ANIMAL", selectedAnimal.name)
            }
            startActivity(intent)
            finish()
        }
    }

    // 動物選擇適配器
    private class AnimalAdapter(
        private val animals: Array<AnimalType>,
        private val onAnimalSelected: (AnimalType) -> Unit
    ) : RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder>() {

        // 為每個動物項目創建動畫處理器
        private val animationHandlers = mutableMapOf<AnimalViewHolder, Handler>()
        private val animationRunnables = mutableMapOf<AnimalViewHolder, Runnable>()
        private val frames = mutableMapOf<AnimalType, Array<Bitmap>>()

        class AnimalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val animalImage: ImageView = view.findViewById(R.id.animalImage)
            val animalName: TextView = view.findViewById(R.id.animalName)
            val animalDescription: TextView = view.findViewById(R.id.animalDescription)
            val selectButton: Button = view.findViewById(R.id.selectButton)
            var currentFrame = 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_animal, parent, false)
            return AnimalViewHolder(view)
        }

        override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
            val animal = animals[position]
            val context = holder.itemView.context

            // 加載動物的待機動畫幀
            if (!frames.containsKey(animal)) {
                frames[animal] = animal.extractFrames(context, true)
            }

            // 設置動物名稱
            holder.animalName.text = animal.displayName

            // 設置動物描述
            val descriptions = mapOf(
                AnimalType.DOBERMAN to "強壯且敏捷的杜賓犬，跳躍能力出色",
                AnimalType.SHIBA to "活潑可愛的柴犬，靈活且反應迅速"
            )
            holder.animalDescription.text = descriptions[animal] ?: ""

            // 顯示第一幀
            holder.animalImage.setImageBitmap(frames[animal]?.get(0))
            holder.currentFrame = 0

            // 設置選擇按鈕
            holder.selectButton.setOnClickListener {
                onAnimalSelected(animal)
            }

            // 停止之前的動畫（如果有）
            animationHandlers[holder]?.removeCallbacks(animationRunnables[holder] ?: return)

            // 創建新的動畫處理器
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    // 更新幀
                    holder.currentFrame = (holder.currentFrame + 1) % (frames[animal]?.size ?: 1)
                    holder.animalImage.setImageBitmap(frames[animal]?.get(holder.currentFrame))

                    // 每200毫秒更新一次
                    handler.postDelayed(this, 200)
                }
            }

            // 保存處理器和可運行對象
            animationHandlers[holder] = handler
            animationRunnables[holder] = runnable

            // 開始動畫
            handler.post(runnable)
        }

        override fun onViewRecycled(holder: AnimalViewHolder) {
            super.onViewRecycled(holder)
            // 當視圖被回收時停止動畫
            animationHandlers[holder]?.removeCallbacks(animationRunnables[holder] ?: return)
        }

        override fun getItemCount() = animals.size
    }
}
