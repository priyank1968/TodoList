package com.gholdencap.todo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.Todo
import com.gholdencap.todo.data.model.UpdateTodoRequest
import com.gholdencap.todo.ui.theme.TodoListTheme

@Composable
fun TodoDetailRoute(
    todoId: Int?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(todoId) {
        if (todoId != null) viewModel.fetchTodo(todoId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TodoDetailScreen(
        isNew = todoId == null,
        uiState = uiState,
        onBack = onBack,
        onSave = { todo, completed, userId ->
            if (todoId == null) {
                viewModel.addTodo(AddTodoRequest(todo = todo, completed = completed, userId = userId))
            } else {
                viewModel.updateTodo(
                    id = todoId,
                    request = UpdateTodoRequest(todo = todo, completed = completed, userId = userId)
                )
            }
            // Offline-first writes complete against Room before this call returns control to the
            // ViewModel's launch block, so it's safe to leave immediately without waiting on the
            // network — the outbox carries the rest.
            onBack()
        },
        onDelete = { id ->
            viewModel.deleteTodo(id)
            onBack()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoDetailScreen(
    isNew: Boolean,
    uiState: TodoDetailUiState,
    onBack: () -> Unit,
    onSave: (todo: String, completed: Boolean, userId: Int) -> Unit,
    onDelete: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New todo" else "Edit todo") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    if (!isNew && uiState is TodoDetailUiState.Success) {
                        TextButton(onClick = { onDelete(uiState.todo.id) }) { Text("Delete") }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            // isNew never touches the ViewModel's uiState, so Loading/Error/Deleted only apply
            // to the edit path — the branch order matters here.
            !isNew && uiState is TodoDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            !isNew && uiState is TodoDetailUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.message)
            }

            !isNew && uiState is TodoDetailUiState.Deleted -> LaunchedEffect(Unit) { onBack() }

            else -> {
                val isPendingSync = (uiState as? TodoDetailUiState.Success)?.isPendingSync == true
                // Seeded once, the first time this branch composes for a given todo — after that
                // the fields are owned by the user, so a background sync remapping the id
                // underneath (placeholder -> server id) can't stomp an in-progress edit.
                val initial = (uiState as? TodoDetailUiState.Success)?.todo
                var text by remember { mutableStateOf(initial?.todo.orEmpty()) }
                var completed by remember { mutableStateOf(initial?.completed == true) }
                var userIdText by remember { mutableStateOf((initial?.userId ?: 1).toString()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    if (isPendingSync) {
                        Text(
                            "Waiting to sync…",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Todo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = completed, onCheckedChange = { completed = it })
                        Text("Completed")
                    }
                    OutlinedTextField(
                        value = userIdText,
                        onValueChange = { userIdText = it.filter(Char::isDigit) },
                        label = { Text("User id") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onSave(text, completed, userIdText.toIntOrNull() ?: 0) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailScreenNewPreview() {
    TodoListTheme {
        TodoDetailScreen(
            isNew = true,
            uiState = TodoDetailUiState.Loading,
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailScreenLoadingPreview() {
    TodoListTheme {
        TodoDetailScreen(
            isNew = false,
            uiState = TodoDetailUiState.Loading,
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailScreenErrorPreview() {
    TodoListTheme {
        TodoDetailScreen(
            isNew = false,
            uiState = TodoDetailUiState.Error("Todo not found"),
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoDetailScreenEditPreview() {
    TodoListTheme {
        TodoDetailScreen(
            isNew = false,
            uiState = TodoDetailUiState.Success(
                todo = Todo(id = 1, todo = "Buy groceries", completed = false, userId = 1),
                isOffline = true,
                isPendingSync = true,
            ),
            onBack = {},
            onSave = { _, _, _ -> },
            onDelete = {},
        )
    }
}
