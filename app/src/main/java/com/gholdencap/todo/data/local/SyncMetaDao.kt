package com.gholdencap.todo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    fun observe(): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    suspend fun get(): SyncMetaEntity?

    @Upsert
    suspend fun upsert(meta: SyncMetaEntity)
}
