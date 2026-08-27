package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add specialtyBar column
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN specialtyBar TEXT DEFAULT NULL")

        // 2. Correct Specialty Bar discriminators
        db.execSQL("UPDATE exercise_definitions SET specialtyBar = 'TRAP' WHERE id = 'trap_bar_deadlift'")
        db.execSQL("UPDATE exercise_definitions SET specialtyBar = 'SAFETY' WHERE id = 'squat_safety_bar'")

        // 3. Taxonomy Correction: Grouping by familyId
        
        // Bench Press
        val benchPressFamilies = listOf("incline_bench_press", "floor_press", "close_grip_press", "close_grip_bench")
        benchPressFamilies.forEach { fam ->
            db.execSQL("UPDATE exercise_definitions SET familyId = 'bench_press', familyName = 'Bench Press' WHERE familyId = '$fam'")
        }

        // Squat
        val squatFamilies = listOf("zercher_squat", "hack_squat", "belt_squat", "pendulum_squat")
        squatFamilies.forEach { fam ->
            db.execSQL("UPDATE exercise_definitions SET familyId = 'squat', familyName = 'Squat' WHERE familyId = '$fam'")
        }

        // Deadlift
        val deadliftFamilies = listOf("romanian_deadlift", "rdl", "good_morning", "back_extension", "hip_thrust", "kettlebell_swing")
        deadliftFamilies.forEach { fam ->
            db.execSQL("UPDATE exercise_definitions SET familyId = 'deadlift', familyName = 'Deadlift' WHERE familyId = '$fam'")
        }

        // Rows
        val rowFamilies = listOf("bent_over_row", "one_arm_row", "seated_row", "tbar_row", "rack_pull", "row")
        rowFamilies.forEach { fam ->
            db.execSQL("UPDATE exercise_definitions SET familyId = 'rows', familyName = 'Row' WHERE familyId = '$fam'")
        }

        // Calves
        db.execSQL("UPDATE exercise_definitions SET familyId = 'calves', familyName = 'Calf' WHERE familyId = 'calf_raise'")
    }
}
