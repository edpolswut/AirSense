package br.airsense.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Estado de sincronização com a nuvem. Por enquanto só o Room é usado,
 * então todo registro fica PENDING até a sincronização existir.
 */
enum class SyncState(val code: Int) {
    SYNCED(0),
    PENDING(1),
    DELETED(2);

    companion object {
        fun fromCode(code: Int): SyncState = entries.firstOrNull { it.code == code } ?: PENDING
    }
}

@Entity(
    tableName = "user",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: Int = SyncState.PENDING.code
) {
    val sync: SyncState
        get() = SyncState.fromCode(syncState)
}