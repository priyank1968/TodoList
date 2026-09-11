package com.gholdencap.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gholdencap.todo.data.repository.TodoRepository
import com.gholdencap.todo.data.sync.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    /** Network-only state; the list itself comes from the cache and is never cleared by a failure. */
    private data class RefreshState(
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    )

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<TodoListUiState> = combine(
        repository.observeTodos(),
        repository.observeSyncStatus(),
        repository.observePendingChangeCount(),
        networkMonitor.isOnline,
        refreshState
    ) { todos, syncStatus, pendingChanges, isOnline, refresh ->
        when {
            todos.isEmpty() && syncStatus == null && refresh.errorMessage == null ->
                TodoListUiState.Loading

            todos.isEmpty() && refresh.errorMessage != null ->
                TodoListUiState.Error(refresh.errorMessage)

            else -> TodoListUiState.Success(
                todos = todos,
                isOffline = !isOnline,
                pendingChanges = pendingChanges,
                isRefreshing = refresh.isRefreshing,
                canLoadMore = syncStatus?.canLoadMore == true,
                refreshError = refresh.errorMessage,
                lastSyncedAt = syncStatus?.lastSyncedAt
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodoListUiState.Loading
    )

    init {
        refresh()
    }

    fun refresh() = load { repository.refresh() }

    fun loadNextPage() {
        if ((uiState.value as? TodoListUiState.Success)?.canLoadMore != true) return
        load { repository.loadNextPage() }
    }

    private fun load(block: suspend () -> Result<Unit>) {
        if (refreshState.value.isRefreshing) return
        refreshState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            val result = block()
            refreshState.value = RefreshState(
                isRefreshing = false,
                errorMessage = result.exceptionOrNull()?.let { it.message ?: "Unknown Error!" }
            )
        }
    }
}
