package com.gholdencap.todo.data.model

data class TodoData(
    val limit: Int,
    val skip: Int,
    val todos: List<Todo>,
    val total: Int
)