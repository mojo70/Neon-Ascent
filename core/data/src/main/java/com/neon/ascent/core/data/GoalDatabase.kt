package com.neon.ascent.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GoalEntity::class], version = 1)
abstract class GoalDatabase : RoomDatabase() {
    abstract fun goalDao(): NewGoalDao
}
