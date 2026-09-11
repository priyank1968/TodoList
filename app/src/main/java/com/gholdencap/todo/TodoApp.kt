package com.gholdencap.todo

import android.app.Application
import com.gholdencap.todo.data.sync.OutboxSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoApp : Application() {

    @Inject
    lateinit var outboxSyncManager: OutboxSyncManager

    override fun onCreate() {
        super.onCreate()
        // Picks up anything left queued by a previous run and drains it once there's a network.
        outboxSyncManager.start()
    }
}
