package com.gholdencap.todo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxOperation { CREATE, UPDATE, DELETE }

/**
 * One not-yet-acknowledged mutation. Rows are sent oldest-first and deleted only once the server
 * accepts them, so an entry surviving a process death is simply retried on the next flush.
 *
 * [todoId] is rewritten from the placeholder id to the server id once a CREATE succeeds, which is
 * what lets an offline create-then-edit sequence land on the right remote row.
 */
@Entity(tableName = "outbox")
data class OutboxEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Int,
    val operation: OutboxOperation,
    val todo: String? = null,
    val completed: Boolean? = null,
    val userId: Int? = null,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null
)
