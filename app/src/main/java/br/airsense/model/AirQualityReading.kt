package br.airsense.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "air_quality_reading",
    indices = [Index(value = ["deviceId"]), Index(value = ["environmentId"])]
)
data class AirQualityReading(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceId: String,
    val environmentId: Int? = null,
    val roomId: Int? = null,
    val temperature: Float,
    val humidity: Float,
    val airQualityIndex: Float, // AQI
    val co2Level: Float,        // ppm
    val timestamp: Long = System.currentTimeMillis()
)