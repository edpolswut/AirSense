package br.airsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device")
data class AirSenseDevice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val deviceId: String = "FakeDevice",
    //val environment: Environment? = null
)