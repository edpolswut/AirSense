package br.airsense.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "environment",
    indices = [Index(value = ["userId"])]
)
data class Environment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val name: String = "",
    val description: String = ""
)

@Entity(
    tableName = "room",
    foreignKeys = [
        ForeignKey(
            entity = Environment::class,
            parentColumns = ["id"],
            childColumns = ["environmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["environmentId"])]
)
data class EnvironmentRoom(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val environmentId: Int,
    val name: String,
    val description: String = "",
    val areaM2: Double = 0.0,

    // US015: com useCustomLimits = false valem os limites padrão do sistema.
    val useCustomLimits: Boolean = false,
    val tempMinSafe: Float = 18.0f,
    val tempMaxSafe: Float = 26.0f,
    val humidityMinSafe: Float = 30.0f,
    val humidityMaxSafe: Float = 60.0f,
    val airQualityMinSafe: Float = 0.0f,
    val airQualityMaxSafe: Float = 100.0f,
    val co2MinSafe: Float = 400.0f,
    val co2MaxSafe: Float = 1000.0f
)