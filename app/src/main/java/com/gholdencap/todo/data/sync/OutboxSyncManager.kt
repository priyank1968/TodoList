package com.gholdencap.todo.data.sync

import androidx.room.withTransaction
import com.gholdencap.todo.data.local.OutboxDao
import com.gholdencap.todo.data.local.OutboxEntryEntity
import com.gholdencap.todo.data.local.OutboxOperation
import com.gholdencap.todo.data.local.TodoDao
import com.gholdencap.todo.data.local.TodoDatabase
import com.gholdencap.todo.data.local.asEntity
import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.UpdateTodoRequest
import com.gholdencap.todo.data.remote.TodoApi
import com.gholdencap.todo.di.ApplicationScope
import com.gholdencap.todo.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drains the outbox against the API. Entries are sent strictly oldest-first so that a create
 * followed by an edit of the same todo reaches the server in that order.
 */
@Singleton
class OutboxSyncManager @Inject constructor(
    private val api: TodoApi,
    private val database: TodoDatabase,
    private val todoDao: TodoDao,
    private val outboxDao: OutboxDao,
    private val networkMonitor: NetworkMonitor,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val flushMutex = Mutex()

    private val _syncState = MutableStateFlow<OutboxSyncState>(OutboxSyncState.Idle)
    val syncState = _syncState.asStateFlow()

    /** Called once from the Application so a queue left over from a previous run gets drained. */
    fun start() {
        networkMonitor.isOnline
            .filter { it }
            .onEach { flush() }
            .launchIn(scope)
    }

    /** Fire-and-forget flush for the write path — the caller shouldn't block on the network. */
    fun requestSync() {
        scope.launch { flush() }
    }

    suspend fun flush() = withContext(ioDispatcher) {
        // A single in-flight flush, otherwise a connectivity callback racing a local write would
        // send the same entry twice.
        flushMutex.withLock {
            _syncState.value = OutboxSyncState.Syncing
            var failure: Throwable? = null

            // Re-read the head each iteration rather than iterating a snapshot: sending a CREATE
            // rewrites the todoId of the entries still queued behind it.
            while (true) {
                val entry = outboxDao.oldestPending() ?: break
                try {
                    send(entry)
                    outboxDao.delete(entry)
                } catch (e: IOException) {
                    // Transport failure: still offline. Leave the entry queued and stop — later
                    // entries may depend on this one having landed.
                    outboxDao.update(entry.withFailure(e))
                    failure = e
                    break
                } catch (e: HttpException) {
                    if (e.code() in 400..499) {
                        // The server rejected it outright; retrying can't help, so drop the entry
                        // instead of wedging the queue behind it.
                        outboxDao.delete(entry)
                    } else {
                        outboxDao.update(entry.withFailure(e))
                        failure = e
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Anything unexpected (a malformed response, say) must not escape into the
                    // application scope, where it would take the process down.
                    outboxDao.update(entry.withFailure(e))
                    failure = e
                    break
                }
            }

            _syncState.value = failure?.let { OutboxSyncState.Failed(it.message ?: "Sync failed") }
                ?: OutboxSyncState.Idle
        }
    }

    private suspend fun send(entry: OutboxEntryEntity) {
        val now = System.currentTimeMillis()
        when (entry.operation) {
            OutboxOperation.CREATE -> {
                val created = api.addTodo(
                    AddTodoRequest(
                        todo = entry.todo.orEmpty(),
                        completed = entry.completed == true,
                        userId = entry.userId ?: 0
                    )
                )
                database.withTransaction {
                    // Swap the placeholder row for one keyed by the id the server just assigned,
                    // and re-point anything queued behind it at that id.
                    todoDao.deleteById(entry.todoId)
                    todoDao.upsert(created.asEntity(now, localId = entry.todoId))
                    outboxDao.remapTodoId(localId = entry.todoId, serverId = created.id)
                }
            }

            OutboxOperation.UPDATE -> {
                val updated = api.updateTodo(
                    id = entry.todoId,
                    request = UpdateTodoRequest(
                        todo = entry.todo,
                        completed = entry.completed,
                        userId = entry.userId
                    )
                )
                // Upsert replaces the whole row, so carry the placeholder id forward.
                val cached = todoDao.getTodo(entry.todoId)
                todoDao.upsert(updated.asEntity(now, localId = cached?.localId))
            }

            OutboxOperation.DELETE -> api.deleteTodo(entry.todoId)
        }
    }

    private fun OutboxEntryEntity.withFailure(cause: Throwable) =
        copy(attemptCount = attemptCount + 1, lastError = cause.message)
}

sealed interface OutboxSyncState {
    data object Idle : OutboxSyncState
    data object Syncing : OutboxSyncState
    data class Failed(val message: String) : OutboxSyncState
}
