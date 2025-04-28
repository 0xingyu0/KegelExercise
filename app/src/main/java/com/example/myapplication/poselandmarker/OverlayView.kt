package com.example.myapplication.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for(landmark in poseLandmarkerResult.landmarks()) {
                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                        linePaint)
                }

                val leftHip = Triple(landmark[23].x() * imageWidth, landmark[23].y() * imageHeight,landmark[23].z() * imageWidth)   // 左臀
                val leftKnee = Triple(landmark[25].x() * imageWidth, landmark[25].y() * imageHeight,landmark[25].z() * imageWidth) // 左膝
                val leftAnkle = Triple(landmark[27].x() * imageWidth, landmark[27].y() * imageHeight,landmark[27].z() * imageWidth) // 左腳踝

                val rightHip = Triple(landmark[24].x() * imageWidth, landmark[24].y() * imageHeight,landmark[24].z() * imageWidth)   // 右臀
                val rightKnee = Triple(landmark[26].x() * imageWidth, landmark[26].y() * imageHeight,landmark[26].z() * imageWidth) // 右膝
                val rightAnkle = Triple(landmark[28].x() * imageWidth, landmark[28].y() * imageHeight,landmark[28].z() * imageWidth) // 右腳踝

                // 計算膝蓋角度
                val leftKneeAngle = AngleCalculator.calculateAngle(leftHip, leftKnee, leftAnkle)
                val rightKneeAngle = AngleCalculator.calculateAngle(rightHip, rightKnee, rightAnkle)

                // 設定繪製文字的 Paint（字體顏色、大小等）
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 50f
                    textAlign = Paint.Align.LEFT
                }

                // 固定顯示在左上角 (20, 60) 和 (20, 120)
                canvas.drawText("左膝角度: %.1f°".format(leftKneeAngle), 20f, 180f, textPaint)
                canvas.drawText("右膝角度: %.1f°".format(rightKneeAngle), 20f, 240f, textPaint)
            }
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}