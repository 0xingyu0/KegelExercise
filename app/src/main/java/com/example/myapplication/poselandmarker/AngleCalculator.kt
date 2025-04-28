package com.example.myapplication.poselandmarker

import kotlin.math.acos
import kotlin.math.sqrt

object AngleCalculator {

    /**
     * 計算三個點之間的 3D 角度
     * @param a 第一個點 (Ax, Ay, Az)
     * @param b 中間點 (Bx, By, Bz) 角度基準點
     * @param c 第三個點 (Cx, Cy, Cz)
     * @return 返回角度 (0 ~ 180 度)
     */
    fun calculateAngle(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, c: Triple<Float, Float, Float>): Double {
        val baX = (a.first - b.first).toDouble()
        val baY = (a.second - b.second).toDouble()
        val baZ = (a.third - b.third).toDouble()

        val bcX = (c.first - b.first).toDouble()
        val bcY = (c.second - b.second).toDouble()
        val bcZ = (c.third - b.third).toDouble()

        val dotProduct = (baX * bcX) + (baY * bcY) + (baZ * bcZ)
        val magnitudeBA = sqrt(baX * baX + baY * baY + baZ * baZ)
        val magnitudeBC = sqrt(bcX * bcX + bcY * bcY + bcZ * bcZ)

        return if (magnitudeBA == 0.0 || magnitudeBC == 0.0) {
            0.0
        } else {
            val cosTheta = dotProduct / (magnitudeBA * magnitudeBC)
            Math.toDegrees(acos(cosTheta.coerceIn(-1.0, 1.0)))  //使用 Java 的 Math.toDegrees()
        }
    }
}
