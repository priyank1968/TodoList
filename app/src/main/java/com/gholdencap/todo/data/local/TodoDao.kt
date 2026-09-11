package com.gholdencap.todo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY cachedAt ASC, id ASC")
    fun observeTodos(): Flow<List<TodoEntity>>

    /** Also matches on the pre-sync placeholder id, so a caller holding the old id still resolves. */
    @Query("SELECT * FROM todos WHERE id = :id OR localId = :id LIMIT 1")
    fun observeTodo(id: Int): Flow<TodoEntity?>

    @Query("SELECT * FROM todos WHERE id = :id OR localId = :id LIMIT 1")
    suspend fun getTodo(id: Int): TodoEntity?

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Upsert
    suspend fun upsertAll(todos: List<TodoEntity>)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Clears the server-backed rows ahead of a page-0 refresh. Rows with a queued mutation are left
     * alone — dropping them would lose an edit the server hasn't seen yet.
     */
    @Query("DELETE FROM todos WHERE id > 0 AND id NOT IN (SELECT todoId FROM outbox)")
    suspend fun deleteSyncedRemoteTodos()

    /** Placeholder ids for offline creates walk downwards from this. */
    @Query("SELECT MIN(id) FROM todos")
    suspend fun minId(): Int?

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    /**
     * Keeps the [keep] most recently cached todos. Rows with a pending mutation are exempt, so the
     * cache can briefly exceed the cap rather than silently discard unsynced work.
     */
    @Query(
        """
        DELETE FROM todos
        WHERE id NOT IN (SELECT id FROM todos ORDER BY cachedAt DESC, id DESC LIMIT :keep)
          AND id NOT IN (SELECT todoId FROM outbox)
        """
    )
    suspend fun trimTo(keep: Int)
}
