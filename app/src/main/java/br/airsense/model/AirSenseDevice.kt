package br.airsense.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device")
data class AirSenseDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val deviceId: String,
    val status: Int = DeviceStatus.ACTIVE.code,
    val environmentId: Int? = null,
    val roomId: Int? = null,

    // Faixas de leitura segura (min e max)
    val tempMinSafe: Float = 18.0f,
    val tempMaxSafe: Float = 26.0f,

    val humidityMinSafe: Float = 30.0f,
    val humidityMaxSafe: Float = 60.0f,

    val airQualityMinSafe: Float = 0.0f,
    val airQualityMaxSafe: Float = 100.0f,

    val co2MinSafe: Float = 400.0f,
    val co2MaxSafe: Float = 1000.0f
) {
    val deviceStatus: DeviceStatus
        get() = DeviceStatus.fromCode(status)

    val badgeColors: Pair<Color, Color>
        get() = deviceStatus.badgeColors
}

object ReadingLimits {
    val TEMP_BOUNDS = -10.0f..50.0f
    val HUMIDITY_BOUNDS = 0.0f..100.0f
    val AIR_QUALITY_BOUNDS = 0.0f..500.0f
    val CO2_BOUNDS = 300.0f..5000.0f
}

enum class DeviceStatus(
    val code: Int,
    val label: String,
    val backgroundColor: Color,
    val contentColor: Color
) {
    OFF(0, "Desligado", Color(0xFFECEFF1), Color(0xFF546E7A)),
    ACTIVE(1, "Ativo", Color(0xFFE8F5E9), Color(0xFF2E7D32)),
    NOT_WORKING(2, "Com defeito", Color(0xFFFFEBEE), Color(0xFFC62828));

    val badgeColors: Pair<Color, Color>
        get() = backgroundColor to contentColor

    companion object {
        fun fromCode(code: Int): DeviceStatus = entries.firstOrNull { it.code == code } ?: OFF
    }
}