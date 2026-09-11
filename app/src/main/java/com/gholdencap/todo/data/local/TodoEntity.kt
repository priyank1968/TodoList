package com.gholdencap.todo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gholdencap.todo.data.model.Todo

/**
 * Cached todo. [id] is the server id for anything that has round-tripped; todos created while
 * offline get a negative placeholder id until the outbox learns the real one from the server.
 *
 * [localId] keeps the placeholder around after that swap, so a screen that opened the todo before
 * it synced can still resolve it by the id it was handed.
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: Int,
    val todo: String,
    val completed: Boolean,
    val userId: Int,
    val localId: Int? = null,
    /** Drives both list order and which rows get evicted once the cache exceeds its cap. */
    val cachedAt: Long
) {
    val isLocalOnly: Boolean get() = id < 0
}

fun TodoEntity.asExternalModel() = Todo(
    id = id,
    todo = todo,
    completed = completed,
    userId = userId
)

fun Todo.asEntity(cachedAt: Long, localId: Int? = null) = TodoEntity(
    id = id,
    todo = todo,
    completed = completed,
    userId = userId,
    localId = localId,
    cachedAt = cachedAt
)
