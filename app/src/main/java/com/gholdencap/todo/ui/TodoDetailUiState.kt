package com.gholdencap.todo.ui

import com.gholdencap.todo.data.model.Todo

sealed interface TodoDetailUiState {

    data object Loading : TodoDetailUiState

    data class Error(val message: String) : TodoDetailUiState

    /** The todo is gone from the cache — either deleted here or absent upstream. */
    data object Deleted : TodoDetailUiState

    data class Success(
        val todo: Todo,
        val isOffline: Boolean,
        /** This todo has a mutation still queued in the outbox. */
        val isPendingSync: Boolean
    ) : TodoDetailUiState
}
