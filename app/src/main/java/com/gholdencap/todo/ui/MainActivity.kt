package com.gholdencap.todo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gholdencap.todo.ui.theme.TodoListTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.collections.listOf

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoListTheme {
                val backStack = remember { mutableStateListOf<Any>(TodoRoute.TodoList) }

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<TodoRoute.TodoList> {
                            TodoListRoute(
                                onTodoClick = { id -> TodoRoute.TodoDetail(id) },
                                onAddClick = { TodoRoute.TodoDetail(null) }
                            )
                        }
                        entry<TodoRoute.TodoDetail> { route ->
                            TodoDetailRoute(
                                todoId = route.id,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}
