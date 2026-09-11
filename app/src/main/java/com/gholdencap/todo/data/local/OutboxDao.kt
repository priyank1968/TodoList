package com.gholdencap.todo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Query("SELECT * FROM outbox ORDER BY id ASC LIMIT 1")
    suspend fun oldestPending(): OutboxEntryEntity?

    @Query("SELECT * FROM outbox WHERE todoId = :todoId ORDER BY id ASC")
    suspend fun entriesFor(todoId: Int): List<OutboxEntryEntity>

    @Query("SELECT COUNT(*) FROM outbox")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox WHERE todoId = :todoId")
    fun observePendingCountFor(todoId: Int): Flow<Int>

    @Insert
    suspend fun insert(entry: OutboxEntryEntity): Long

    @Update
    suspend fun update(entry: OutboxEntryEntity)

    @Delete
    suspend fun delete(entry: OutboxEntryEntity)

    @Query("DELETE FROM outbox WHERE todoId = :todoId")
    suspend fun deleteAllFor(todoId: Int)

    /** Re-points queued follow-up mutations once a CREATE comes back with the real server id. */
    @Query("UPDATE outbox SET todoId = :serverId WHERE todoId = :localId")
    suspend fun remapTodoId(localId: Int, serverId: Int)
}
