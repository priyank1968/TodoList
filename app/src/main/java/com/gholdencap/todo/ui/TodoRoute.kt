package com.gholdencap.todo.ui

/**
 * Navigation 3 routes for the todo flow. Plain classes rather than NavKey/@Serializable — this is
 * a single-activity app with no need to survive process death, so the extra kotlinx.serialization
 * setup a saveable back stack would need isn't worth it (see nav3-recipes' BasicActivity vs
 * BasicSaveableActivity).
 */
sealed interface TodoRoute {
    data object TodoList : TodoRoute
    data class TodoDetail(val id: Int?) : TodoRoute
}
