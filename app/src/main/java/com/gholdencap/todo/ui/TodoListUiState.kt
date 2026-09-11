package com.gholdencap.todo.ui

import com.gholdencap.todo.data.model.Todo

sealed interface TodoListUiState {

    /** Nothing cached yet and the first refresh hasn't come back. */
    data object Loading : TodoListUiState

    /** Only reachable with an empty cache — once anything is cached, a failed refresh degrades to
     *  [Success] with [Success.refreshError] set rather than blanking the screen. */
    data class Error(val message: String) : TodoListUiState

    data class Success(
        val todos: List<Todo>,
        val isOffline: Boolean,
        val pendingChanges: Int,
        val isRefreshing: Boolean,
        val canLoadMore: Boolean,
        val refreshError: String? = null,
        val lastSyncedAt: Long? = null
    ) : TodoListUiState
}
