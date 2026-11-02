package br.airsense.util

import androidx.compose.ui.graphics.Color

enum class AirQualityStatus(val label: String, val color: Color) {
    GOOD("Boa", Color(0xFF2E7D32)),      // Verde
    MODERATE("Moderada", Color(0xFFF57F17)), // Amarelo/Laranja
    BAD("Ruim / Crítica", Color(0xFFC62828)) // Vermelho
}

object AirQualityClassifier {
    fun classify(aqi: Float): AirQualityStatus {
        return when {
            aqi <= 50f -> AirQualityStatus.GOOD
            aqi <= 100f -> AirQualityStatus.MODERATE
            else -> AirQualityStatus.BAD
        }
    }
}