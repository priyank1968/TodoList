package com.gholdencap.todo.data.repository

import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.SyncStatus
import com.gholdencap.todo.data.model.Todo
import com.gholdencap.todo.data.model.UpdateTodoRequest
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: reads always come from the local cache and never touch the network, so they cannot
 * fail. Writes land locally first and are queued in the outbox for the server.
 */
interface TodoRepository {

    fun observeTodos(): Flow<List<Todo>>

    fun observeTodo(id: Int): Flow<Todo?>

    /** Null until the first successful refresh. */
    fun observeSyncStatus(): Flow<SyncStatus?>

    fun observePendingChangeCount(): Flow<Int>

    fun observeHasPendingChange(id: Int): Flow<Boolean>

    /** Re-fetches page 0, replacing the cached remote rows. */
    suspend fun refresh(): Result<Unit>

    suspend fun loadNextPage(): Result<Unit>

    /** @return the local id the new todo was cached under; negative until the server assigns one. */
    suspend fun addTodo(request: AddTodoRequest): Int

    suspend fun updateTodo(id: Int, request: UpdateTodoRequest)

    suspend fun deleteTodo(id: Int)
}
