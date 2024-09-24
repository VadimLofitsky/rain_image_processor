package lofitsky.filler.common

import lofitsky.filler.FillerApp.Companion.WORK_IMG_H
import lofitsky.filler.FillerApp.Companion.WORK_IMG_W
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class FieldInfo {
    companion object {
        private const val JOIN_THRESHOLD = 50
        const val JOIN_THRESHOLD2 = JOIN_THRESHOLD * JOIN_THRESHOLD

        const val SIMILARITY_THRESHOLD = 0.37

        private const val FULL_IMAGE_WEIGHT = WORK_IMG_W.toDouble() * WORK_IMG_H
        private val FULL_IMAGE_DIAG = sqrt(WORK_IMG_W.toDouble() * WORK_IMG_W + WORK_IMG_H.toDouble() * WORK_IMG_H)
        private const val WEIGHT_WEIGHT = 0.25
        private const val WEIGHT_FACTOR = WEIGHT_WEIGHT / FULL_IMAGE_WEIGHT
        private const val DISTANCE_WEIGHT = 1.0 - WEIGHT_WEIGHT
        private val DISTANCE_FACTOR = DISTANCE_WEIGHT / FULL_IMAGE_DIAG
    }

    var prev: FieldInfo? = null

    var weight = 0
        private set

    var massCenter = 0.0 to 0.0
        private set

    val border = HashSet<Pair<Int, Int>>()

    fun add(x: Int, y: Int, amount: Int = 1): Unit {
        val first = (massCenter.first * weight + x * amount) / (weight + amount)
        val second = (massCenter.second * weight + y * amount) / (weight + amount)
        massCenter = first to second
        weight += amount
    }

    fun add(other: FieldInfo): FieldInfo {
        add(other.massCenter.first.toInt(), other.massCenter.second.toInt(), other.weight)
        return this
    }

    fun distanceTo(other: FieldInfo): Double
        = sqrt((massCenter.first - other.massCenter.first).pow(2) + (massCenter.second - other.massCenter.second).pow(2))

    fun diffTo(other: FieldInfo): Double
        = WEIGHT_FACTOR * abs(weight - other.weight) + DISTANCE_FACTOR * distanceTo(other)

    override fun toString(): String = "(%.1f, %.1f); W(%d); P(%d)".format(Locale.US, massCenter.first, massCenter.second, weight, border.size)
}
