package com.gholdencap.todo.di

import android.content.Context
import androidx.room.Room
import com.gholdencap.todo.data.local.OutboxDao
import com.gholdencap.todo.data.local.SyncMetaDao
import com.gholdencap.todo.data.local.TodoDao
import com.gholdencap.todo.data.local.TodoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesTodoDatabase(@ApplicationContext context: Context): TodoDatabase =
        Room.databaseBuilder(context, TodoDatabase::class.java, "todo.db")
            .build()

    @Provides
    fun providesTodoDao(database: TodoDatabase): TodoDao = database.todoDao()

    @Provides
    fun providesOutboxDao(database: TodoDatabase): OutboxDao = database.outboxDao()

    @Provides
    fun providesSyncMetaDao(database: TodoDatabase): SyncMetaDao = database.syncMetaDao()
}
