package com.gholdencap.todo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gholdencap.todo.data.model.SyncStatus

/**
 * Single-row table holding paging bookkeeping. [loadedFromRemote] is tracked separately from the
 * cached row count because eviction trims the cache without meaning those pages need re-fetching.
 */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val remoteTotal: Int,
    val loadedFromRemote: Int,
    val lastSyncedAt: Long
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

fun SyncMetaEntity.asExternalModel() = SyncStatus(
    remoteTotal = remoteTotal,
    loadedFromRemote = loadedFromRemote,
    lastSyncedAt = lastSyncedAt
)
