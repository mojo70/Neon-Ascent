package com.neon.ascent.data.repository

import com.neon.ascent.data.local.GoalTaskDao
import com.neon.ascent.domain.model.Task
import com.neon.ascent.domain.model.toDomain
import com.neon.ascent.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val goalTaskDao: GoalTaskDao
) {

    fun getTasksForGoal(goalId: String): Flow<List<Task>> =
        goalTaskDao.getTasksForGoal(goalId).map { list -> list.map { it.toDomain() } }

    fun getDailyTasks(): Flow<List<Task>> =
        goalTaskDao.getDailyTasks().map { list -> list.map { it.toDomain() } }

    suspend fun createTask(task: Task) {
        goalTaskDao.upsertTask(task.toEntity())
    }

    suspend fun markTaskCompleted(taskId: String, date: LocalDate = LocalDate.now()) {
        val task = goalTaskDao.getTaskById(taskId).firstOrNull()?.toDomain() ?: return
        val dates = task.completedDates.toMutableList()
        if (!dates.contains(date)) {
            dates.add(date)
        }
        goalTaskDao.markCompleted(taskId, dates.map { it.toString() })
    }
}
