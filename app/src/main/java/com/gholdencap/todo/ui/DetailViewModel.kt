package com.gholdencap.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.UpdateTodoRequest
import com.gholdencap.todo.data.repository.TodoRepository
import com.gholdencap.todo.data.sync.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val todoId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<TodoDetailUiState> = todoId.flatMapLatest { id ->
        if (id == null) {
            flowOf(TodoDetailUiState.Loading)
        } else {
            repository.observeTodo(id).flatMapLatest { todo ->
                if (todo == null) {
                    flowOf(TodoDetailUiState.Deleted)
                } else {
                    // Keyed off todo.id rather than the requested id: once a queued create syncs,
                    // the outbox entries have moved to the server id.
                    combine(
                        repository.observeHasPendingChange(todo.id),
                        networkMonitor.isOnline
                    ) { isPendingSync, isOnline ->
                        TodoDetailUiState.Success(
                            todo = todo,
                            isOffline = !isOnline,
                            isPendingSync = isPendingSync
                        )
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodoDetailUiState.Loading
    )

    fun fetchTodo(id: Int) {
        todoId.value = id
    }

    /**
     * The new todo is cached under a placeholder id straight away; the outbox swaps in the server
     * id later, and following it here keeps this screen pointed at the same row when it does.
     */
    fun addTodo(request: AddTodoRequest) {
        viewModelScope.launch {
            todoId.value = repository.addTodo(request)
        }
    }

    fun updateTodo(id: Int, request: UpdateTodoRequest) {
        viewModelScope.launch { repository.updateTodo(id, request) }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch { repository.deleteTodo(id) }
    }
}
