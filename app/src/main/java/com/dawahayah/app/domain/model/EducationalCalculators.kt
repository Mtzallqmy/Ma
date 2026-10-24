package com.dawahayah.app.domain.model

import kotlin.math.pow

/** Arithmetic training only: no patient characteristics, drug selection or dosing recommendations. */
object EducationalCalculators {
    enum class Unit(val dimension: String, val baseFactor: Double) {
        G("mass", 1.0), MG("mass", 0.001), MCG("mass", 0.000001),
        L("volume", 1.0), ML("volume", 0.001)
    }
    private fun bounded(value: Double, positive: Boolean = false): Double {
        require(value.isFinite() && value >= 0 && value <= 1e12 && (!positive || value > 0)) { "Value outside the educational model limits" }
        return value
    }
    fun convert(value: Double, from: Unit, to: Unit): Double {
        bounded(value); require(from.dimension == to.dimension) { "Different dimensions" }
        return bounded(value * from.baseFactor / to.baseFactor)
    }
    fun dilutionStockVolume(stockMgPerMl: Double, targetMgPerMl: Double, finalMl: Double): Double {
        bounded(stockMgPerMl, true); bounded(targetMgPerMl, true); bounded(finalMl, true)
        require(targetMgPerMl <= stockMgPerMl) { "Dilution cannot increase concentration" }
        return bounded(targetMgPerMl / stockMgPerMl * finalMl, true)
    }
    fun remainingPercent(elapsedHours: Double, halfLifeHours: Double): Double {
        bounded(elapsedHours); bounded(halfLifeHours, true)
        val intervals = elapsedHours / halfLifeHours
        require(intervals <= 100) { "Outside the model's displayed precision" }
        return 100.0 * 0.5.pow(intervals)
    }
}
