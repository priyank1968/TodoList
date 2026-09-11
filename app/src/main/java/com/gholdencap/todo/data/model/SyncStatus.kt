package com.gholdencap.todo.data.model

data class SyncStatus(
    val remoteTotal: Int,
    val loadedFromRemote: Int,
    val lastSyncedAt: Long
) {
    val canLoadMore: Boolean get() = loadedFromRemote < remoteTotal
}
