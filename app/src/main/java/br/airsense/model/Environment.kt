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
    val description: String = ""
)