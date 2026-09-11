package com.gholdencap.todo.data.model

data class Todo(
    val completed: Boolean,
    val id: Int,
    val todo: String,
    val userId: Int
)