package com.gholdencap.todo.data.model

data class UpdateTodoRequest(
    val todo: String? = null,
    val completed: Boolean? = null,
    val userId: Int? = null
)
