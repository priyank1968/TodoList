package com.gholdencap.todo.data.model

data class AddTodoRequest(
    val todo: String,
    val completed: Boolean = false,
    val userId: Int
)
