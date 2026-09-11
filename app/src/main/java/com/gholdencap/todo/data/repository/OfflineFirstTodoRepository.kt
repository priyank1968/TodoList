package com.gholdencap.todo.data.repository

import androidx.room.withTransaction
import com.gholdencap.todo.data.local.OutboxDao
import com.gholdencap.todo.data.local.OutboxEntryEntity
import com.gholdencap.todo.data.local.OutboxOperation
import com.gholdencap.todo.data.local.SyncMetaDao
import com.gholdencap.todo.data.local.SyncMetaEntity
import com.gholdencap.todo.data.local.TodoDao
import com.gholdencap.todo.data.local.TodoDatabase
import com.gholdencap.todo.data.local.TodoEntity
import com.gholdencap.todo.data.local.asEntity
import com.gholdencap.todo.data.local.asExternalModel
import com.gholdencap.todo.data.model.AddTodoRequest
import com.gholdencap.todo.data.model.SyncStatus
import com.gholdencap.todo.data.model.Todo
import com.gholdencap.todo.data.model.UpdateTodoRequest
import com.gholdencap.todo.data.remote.TodoApi
import com.gholdencap.todo.data.sync.OutboxSyncManager
import com.gholdencap.todo.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstTodoRepository @Inject constructor(
    private val api: TodoApi,
    private val database: TodoDatabase,
    private val todoDao: TodoDao,
    private val outboxDao: OutboxDao,
    private val syncMetaDao: SyncMetaDao,
    private val outboxSyncManager: OutboxSyncManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TodoRepository {

    override fun observeTodos(): Flow<List<Todo>> =
        todoDao.observeTodos().map { entities -> entities.map(TodoEntity::asExternalModel) }

    override fun observeTodo(id: Int): Flow<Todo?> =
        todoDao.observeTodo(id).map { it?.asExternalModel() }

    override fun observeSyncStatus(): Flow<SyncStatus?> =
        syncMetaDao.observe().map { it?.asExternalModel() }

    override fun observePendingChangeCount(): Flow<Int> = outboxDao.observePendingCount()

    override fun observeHasPendingChange(id: Int): Flow<Boolean> =
        outboxDao.observePendingCountFor(id).map { it > 0 }

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val response = api.getTodos(limit = PAGE_SIZE, skip = 0)
            val now = System.currentTimeMillis()
            database.withTransaction {
                todoDao.deleteSyncedRemoteTodos()
                todoDao.upsertAll(response.todos.map { it.asEntity(now) })
                syncMetaDao.upsert(
                    SyncMetaEntity(
                        remoteTotal = response.total,
                        loadedFromRemote = response.todos.size,
                        lastSyncedAt = now
                    )
                )
                todoDao.trimTo(MAX_CACHED_TODOS)
            }
        }
    }

    override suspend fun loadNextPage(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // Paged off the count fetched so far, not the cached row count — eviction trims the
            // cache without meaning those pages are owed again.
            val meta = syncMetaDao.get()
            val skip = meta?.loadedFromRemote ?: 0
            if (meta != null && skip >= meta.remoteTotal) return@runCatching

            val response = api.getTodos(limit = PAGE_SIZE, skip = skip)
            val now = System.currentTimeMillis()
            database.withTransaction {
                todoDao.upsertAll(response.todos.map { it.asEntity(now) })
                syncMetaDao.upsert(
                    SyncMetaEntity(
                        remoteTotal = response.total,
                        loadedFromRemote = skip + response.todos.size,
                        lastSyncedAt = now
                    )
                )
                todoDao.trimTo(MAX_CACHED_TODOS)
            }
        }
    }

    override suspend fun addTodo(request: AddTodoRequest): Int = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val localId = database.withTransaction {
            // Placeholder ids walk downwards from -1 so they can never collide with a server id.
            val id = (todoDao.minId() ?: 0).coerceAtMost(0) - 1
            todoDao.upsert(
                TodoEntity(
                    id = id,
                    todo = request.todo,
                    completed = request.completed,
                    userId = request.userId,
                    cachedAt = now
                )
            )
            outboxDao.insert(
                OutboxEntryEntity(
                    todoId = id,
                    operation = OutboxOperation.CREATE,
                    todo = request.todo,
                    completed = request.completed,
                    userId = request.userId,
                    createdAt = now
                )
            )
            id
        }
        outboxSyncManager.requestSync()
        localId
    }

    override suspend fun updateTodo(id: Int, request: UpdateTodoRequest) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val current = todoDao.getTodo(id) ?: return@withTransaction
                val updated = current.copy(
                    todo = request.todo ?: current.todo,
                    completed = request.completed ?: current.completed,
                    userId = request.userId ?: current.userId
                )
                todoDao.upsert(updated)

                // Queued entries carry the full post-edit snapshot rather than a patch, so repeated
                // edits collapse into one request instead of a chain the server has to replay.
                val queued = outboxDao.entriesFor(id)
                queued.filter { it.operation == OutboxOperation.UPDATE }
                    .forEach { outboxDao.delete(it) }

                val pendingCreate = queued.firstOrNull { it.operation == OutboxOperation.CREATE }
                if (pendingCreate != null) {
                    // The server has never seen this todo, so fold the edit into the create.
                    outboxDao.update(
                        pendingCreate.copy(
                            todo = updated.todo,
                            completed = updated.completed,
                            userId = updated.userId
                        )
                    )
                } else {
                    outboxDao.insert(
                        OutboxEntryEntity(
                            todoId = id,
                            operation = OutboxOperation.UPDATE,
                            todo = updated.todo,
                            completed = updated.completed,
                            userId = updated.userId,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            outboxSyncManager.requestSync()
        }
    }

    override suspend fun deleteTodo(id: Int) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val hadPendingCreate = outboxDao.entriesFor(id)
                    .any { it.operation == OutboxOperation.CREATE }

                todoDao.deleteById(id)
                outboxDao.deleteAllFor(id)

                // A todo created and deleted while offline never reached the server, so there is
                // nothing to send — anything else needs a DELETE queued.
                if (!hadPendingCreate) {
                    outboxDao.insert(
                        OutboxEntryEntity(
                            todoId = id,
                            operation = OutboxOperation.DELETE,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            outboxSyncManager.requestSync()
        }
    }

    companion object {
        const val PAGE_SIZE = 30
        const val MAX_CACHED_TODOS = 150
    }
}
