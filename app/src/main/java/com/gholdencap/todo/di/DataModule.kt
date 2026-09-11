package com.gholdencap.todo.di

import com.gholdencap.todo.data.repository.OfflineFirstTodoRepository
import com.gholdencap.todo.data.repository.TodoRepository
import com.gholdencap.todo.data.sync.ConnectivityManagerNetworkMonitor
import com.gholdencap.todo.data.sync.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsTodoRepository(impl: OfflineFirstTodoRepository): TodoRepository

    @Binds
    @Singleton
    abstract fun bindsNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor
}
