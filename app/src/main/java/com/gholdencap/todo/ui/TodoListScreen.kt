package com.gholdencap.todo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gholdencap.todo.data.model.Todo
import com.gholdencap.todo.ui.theme.TodoListTheme

@Composable
fun TodoListRoute(
    onTodoClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodoListScreen(
        uiState = uiState,
        onTodoClick = onTodoClick,
        onAddClick = onAddClick,
        onRetry = viewModel::refresh,
        onLoadMore = viewModel::loadNextPage,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoListScreen(
    uiState: TodoListUiState,
    onTodoClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Todos") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) { Text("+") }
        }
    ) { innerPadding ->
        when (uiState) {
            TodoListUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is TodoListUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.message)
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }

            is TodoListUiState.Success -> Column(Modifier.fillMaxSize().padding(innerPadding)) {
                StatusBanner(uiState)
                LazyColumn(Modifier.weight(1f)) {
                    items(uiState.todos, key = { it.id }) { todo ->
                        TodoRow(todo = todo, onClick = { onTodoClick(todo.id) })
                    }
                    if (uiState.canLoadMore) {
                        item {
                            Button(
                                onClick = onLoadMore,
                                enabled = !uiState.isRefreshing,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Text(if (uiState.isRefreshing) "Loading…" else "Load more")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(state: TodoListUiState.Success) {
    if (!state.isOffline && state.pendingChanges == 0 && state.refreshError == null) return

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (state.isOffline) {
            Text("Offline — showing cached todos", style = MaterialTheme.typography.bodySmall)
        }
        if (state.pendingChanges > 0) {
            Text(
                "${state.pendingChanges} change(s) waiting to sync",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.refreshError != null) {
            Text(
                "Refresh failed: ${state.refreshError}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = todo.todo,
                textDecoration = if (todo.completed) TextDecoration.LineThrough else null
            )
        },
        supportingContent = { Text("#${todo.id}") },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private val previewTodos = listOf(
    Todo(id = 1, todo = "Buy groceries", completed = false, userId = 1),
    Todo(id = 2, todo = "Finish the report", completed = true, userId = 1),
    Todo(id = 3, todo = "Call the dentist", completed = false, userId = 1),
)

@Preview(showBackground = true)
@Composable
private fun TodoListScreenLoadingPreview() {
    TodoListTheme {
        TodoListScreen(
            uiState = TodoListUiState.Loading,
            onTodoClick = {},
            onAddClick = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoListScreenErrorPreview() {
    TodoListTheme {
        TodoListScreen(
            uiState = TodoListUiState.Error("Unable to reach the server"),
            onTodoClick = {},
            onAddClick = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoListScreenSuccessPreview() {
    TodoListTheme {
        TodoListScreen(
            uiState = TodoListUiState.Success(
                todos = previewTodos,
                isOffline = true,
                pendingChanges = 2,
                isRefreshing = false,
                canLoadMore = true,
            ),
            onTodoClick = {},
            onAddClick = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}
