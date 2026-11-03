package br.airsense.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.airsense.model.AirQualityReading
import br.airsense.model.AirSenseDevice
import br.airsense.model.DeviceStatus

/** Semáforo (US010). Cada cor tem um ícone para quem não distingue cores (RNF011). */
enum class AirQualityStatus(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    /** Faixa de AQI que leva a esta cor, mostrada na legenda. */
    val aqiRange: String
) {
    GOOD("Bom", Color(0xFF2E7D32), Icons.Filled.CheckCircle, "AQI de 0 a 50"),
    MODERATE("Alerta", Color(0xFFF57F17), Icons.Filled.Warning, "AQI de 51 a 100"),
    BAD("Crítico", Color(0xFFC62828), Icons.Filled.Error, "AQI acima de 100"),
    NO_DATA("Sem dados", Color(0xFF78909C), Icons.AutoMirrored.Outlined.HelpOutline, "Nenhum dispositivo ativo com leitura")
}

object AirQualityClassifier {
    const val GOOD_MAX_AQI = 50f
    const val MODERATE_MAX_AQI = 100f

    fun classify(aqi: Float): AirQualityStatus {
        return when {
            aqi <= GOOD_MAX_AQI -> AirQualityStatus.GOOD
            aqi <= MODERATE_MAX_AQI -> AirQualityStatus.MODERATE
            else -> AirQualityStatus.BAD
        }
    }
}

/**
 * Estado atual de um grupo de dispositivos (ambiente ou cômodo): média da leitura mais
 * recente de cada dispositivo ativo (US008 e US019). Valores nulos = ainda sem leitura.
 */
data class AirQualitySnapshot(
    val aqi: Float? = null,
    val temperature: Float? = null,
    val humidity: Float? = null,
    val co2: Float? = null,
    val devicesWithData: Int = 0
) {
    val status: AirQualityStatus
        get() = aqi?.let(AirQualityClassifier::classify) ?: AirQualityStatus.NO_DATA

    companion object {
        /** Leitura mais recente de cada dispositivo, pelo identificador (sem diferenciar maiúsculas). */
        fun latestByDevice(readings: List<AirQualityReading>): Map<String, AirQualityReading> =
            readings
                .groupBy { it.deviceId.lowercase() }
                .mapValues { (_, list) -> list.maxBy { it.timestamp } }

        fun of(devices: List<AirSenseDevice>, latest: Map<String, AirQualityReading>): AirQualitySnapshot {
            val current = devices
                .filter { it.deviceStatus == DeviceStatus.ACTIVE }
                .mapNotNull { latest[it.deviceId.lowercase()] }
            if (current.isEmpty()) return AirQualitySnapshot()
            return AirQualitySnapshot(
                aqi = current.map { it.airQualityIndex }.average().toFloat(),
                temperature = current.map { it.temperature }.average().toFloat(),
                humidity = current.map { it.humidity }.average().toFloat(),
                co2 = current.map { it.co2Level }.average().toFloat(),
                devicesWithData = current.size
            )
        }
    }
}
