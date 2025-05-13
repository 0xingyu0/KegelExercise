package com.example.myapplication.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.example.myapplication.R

// 动物类型枚举
enum class AnimalType(
    val displayName: String,
    val idleSpriteResId: Int,
    val idleFrameCount: Int,
    val walkSpriteResId: Int,
    val walkFrameCount: Int,
    val backgroundResId: Int
) {
    DOBERMAN("杜賓", R.drawable.dog1_idle, 4, R.drawable.dog1_walk, 6, R.drawable.game_background),
    SHIBA("柴犬", R.drawable.dog2_idle, 4, R.drawable.dog2_walk, 6, R.drawable.game_background);

    // 从sprite sheet中提取帧
    fun extractFrames(context: Context, isIdle: Boolean = false): Array<Bitmap> {
        val resId = if (isIdle) idleSpriteResId else walkSpriteResId
        val frameCount = if (isIdle) idleFrameCount else walkFrameCount

        val spriteSheet = BitmapFactory.decodeResource(context.resources, resId)
        val frameWidth = spriteSheet.width / frameCount
        val frameHeight = spriteSheet.height

        return Array(frameCount) { frameIndex ->
            val srcRect = Rect(
                frameIndex * frameWidth,
                0,
                (frameIndex + 1) * frameWidth,
                frameHeight
            )

            // 创建目标位图并裁剪
            val frameBitmap = Bitmap.createBitmap(
                frameWidth,
                frameHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = android.graphics.Canvas(frameBitmap)
            canvas.drawBitmap(
                spriteSheet,
                srcRect,
                Rect(0, 0, frameWidth, frameHeight),
                null
            )

            frameBitmap
        }
    }

    // 获取跳跃图片（使用第一帧）
    fun getJumpFrame(context: Context): Bitmap {
        // 使用待機的第一帧作为跳跃图
        val frames = extractFrames(context, true)
        return frames[0]
    }
}
