package com.gholdencap.todo.data.remote

import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.Todo
import com.gholdencap.todo.data.model.TodoData
import com.gholdencap.todo.data.model.UpdateTodoRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TodoApi {

    @GET("todos")
    suspend fun getTodos(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): TodoData

    @GET("todos/{id}")
    suspend fun getTodo(@Path("id")id: Int) : Todo

    @POST("todos/add")
    suspend fun addTodo(@Body request: AddTodoRequest): Todo

    @PUT("todos/{id}")
    suspend fun updateTodo(@Path("id") id: Int, @Body request: UpdateTodoRequest): Todo

    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id")id: Int) : Todo



}