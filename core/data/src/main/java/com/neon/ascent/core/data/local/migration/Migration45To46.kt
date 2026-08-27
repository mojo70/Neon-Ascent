package com.neon.ascent.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_45_46 = object : Migration(45, 46) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add new columns
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN familyId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN familyName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN implement TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN stance TEXT NOT NULL DEFAULT 'STANDARD'")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN allowsAddedLoad INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercise_definitions ADD COLUMN isPrimaryVariant INTEGER NOT NULL DEFAULT 0")

        // 2. Backfill existing data
        // CHECKLIST: Old ID -> familyId mapping for pre-existing catalog
        // bench_press -> bench_press (Bench Press)
        // bench_press_dumbbell -> bench_press (Bench Press)
        // incline_bench_press -> incline_bench_press (Incline Bench Press)
        // incline_bench_press_dumbbell -> incline_bench_press (Incline Bench Press)
        // chest_press_hammer_strength -> chest_press_machine (Chest Press (Machine))
        // incline_smith_press -> incline_bench_press (Incline Bench Press)
        // floor_press_dumbbell -> floor_press (Floor Press)
        // chest_press_plate_loaded -> chest_press_machine (Chest Press (Machine))
        // chest_fly_cable -> chest_fly (Chest Fly)
        // pushup_bodyweight -> pushup (Push Up)
        // weighted_pullups -> pull_up (Pull-Up)
        // chinup_weighted -> chin_up (Chin Up)
        // military_press -> overhead_press (Overhead Press)
        // back_squat -> squat (Squat)
        // deadlift -> deadlift (Deadlift)
        // romanian_deadlift -> romanian_deadlift (Romanian Deadlift)
        // calf_raise -> calf_raise (Calf Raise)
        // bicep_curl_barbell -> bicep_curl (Bicep Curl)
        // jerry_curl -> jerry_curl (Jerry Curl)
        // hammer_curl -> hammer_curl (Hammer Curl)
        // tricep_pushdown_cable -> tricep_pushdown (Tricep Pushdown)
        // db_tricep_extension -> tricep_extension (Tricep Extension)
        // skull_crusher -> skull_crusher (Skull Crusher)
        // weighted_dip -> dip (Dip)
        
        val updates = mapOf(
            "bench_press" to "UPDATE exercise_definitions SET familyId='bench_press', familyName='Bench Press', implement='BARBELL', stance='STANDARD', isPrimaryVariant=1 WHERE id='bench_press'",
            "bench_press_dumbbell" to "UPDATE exercise_definitions SET familyId='bench_press', familyName='Bench Press', implement='DUMBBELL' WHERE id='bench_press_dumbbell'",
            "incline_bench_press" to "UPDATE exercise_definitions SET familyId='incline_bench_press', familyName='Incline Bench Press', implement='BARBELL', stance='INCLINE', isPrimaryVariant=1 WHERE id='incline_bench_press'",
            "incline_bench_press_dumbbell" to "UPDATE exercise_definitions SET familyId='incline_bench_press', familyName='Incline Bench Press', implement='DUMBBELL', stance='INCLINE' WHERE id='incline_bench_press_dumbbell'",
            "chest_press_hammer_strength" to "UPDATE exercise_definitions SET familyId='chest_press_machine', familyName='Chest Press (Machine)', implement='PLATE_LOADED' WHERE id='chest_press_hammer_strength'",
            "incline_smith_press" to "UPDATE exercise_definitions SET familyId='incline_bench_press', familyName='Incline Bench Press', implement='SMITH', stance='INCLINE' WHERE id='incline_smith_press'",
            "floor_press_dumbbell" to "UPDATE exercise_definitions SET familyId='floor_press', familyName='Floor Press', implement='DUMBBELL', stance='FLOOR' WHERE id='floor_press_dumbbell'",
            "chest_press_plate_loaded" to "UPDATE exercise_definitions SET familyId='chest_press_machine', familyName='Chest Press (Machine)', implement='PLATE_LOADED' WHERE id='chest_press_plate_loaded'",
            "chest_fly_cable" to "UPDATE exercise_definitions SET familyId='chest_fly', familyName='Chest Fly', implement='CABLE' WHERE id='chest_fly_cable'",
            "pushup_bodyweight" to "UPDATE exercise_definitions SET familyId='pushup', familyName='Push Up', implement='BODYWEIGHT' WHERE id='pushup_bodyweight'",
            "decline_pushup_bodyweight" to "UPDATE exercise_definitions SET familyId='pushup', familyName='Push Up', implement='BODYWEIGHT', stance='DECLINE' WHERE id='decline_pushup_bodyweight'",
            "atlas_pushup_bodyweight" to "UPDATE exercise_definitions SET familyId='pushup', familyName='Push Up', implement='BODYWEIGHT', stance='DEFICIT' WHERE id='atlas_pushup_bodyweight'",
            "pullup_bodyweight" to "UPDATE exercise_definitions SET familyId='pull_up', familyName='Pull-Up', implement='BODYWEIGHT', allowsAddedLoad=0 WHERE id='pullup_bodyweight'",
            "weighted_pullups" to "UPDATE exercise_definitions SET familyId='pull_up', familyName='Pull-Up', implement='BODYWEIGHT', allowsAddedLoad=1, isPrimaryVariant=1 WHERE id='weighted_pullups'",
            "chinup_bodyweight" to "UPDATE exercise_definitions SET familyId='chin_up', familyName='Chin Up', implement='BODYWEIGHT', allowsAddedLoad=0 WHERE id='chinup_bodyweight'",
            "chinup_weighted" to "UPDATE exercise_definitions SET familyId='chin_up', familyName='Chin Up', implement='BODYWEIGHT', allowsAddedLoad=1, isPrimaryVariant=1 WHERE id='chinup_weighted'",
            "lat_pulldown" to "UPDATE exercise_definitions SET familyId='lat_pulldown', familyName='Lat Pulldown', implement='CABLE' WHERE id='lat_pulldown'",
            "seated_row" to "UPDATE exercise_definitions SET familyId='seated_row', familyName='Seated Row', implement='CABLE', stance='SEATED' WHERE id='seated_row'",
            "lat_row_plate_loaded" to "UPDATE exercise_definitions SET familyId='seated_row', familyName='Seated Row', implement='PLATE_LOADED' WHERE id='lat_row_plate_loaded'",
            "facepull_cable" to "UPDATE exercise_definitions SET familyId='facepull', familyName='Face Pull', implement='CABLE' WHERE id='facepull_cable'",
            "one_arm_row_dumbbell" to "UPDATE exercise_definitions SET familyId='one_arm_row', familyName='One-Arm Row', implement='DUMBBELL', stance='SINGLE_ARM' WHERE id='one_arm_row_dumbbell'",
            "bent_over_row" to "UPDATE exercise_definitions SET familyId='bent_over_row', familyName='Bent-Over Row', implement='BARBELL', isPrimaryVariant=1 WHERE id='bent_over_row'",
            "tbar_row_chest_supported" to "UPDATE exercise_definitions SET familyId='tbar_row', familyName='T-Bar Row', implement='PLATE_LOADED', stance='CHEST_SUPPORTED' WHERE id='tbar_row_chest_supported'",
            "rack_pull_below_knee" to "UPDATE exercise_definitions SET familyId='rack_pull', familyName='Rack Pull', implement='BARBELL', stance='DEFICIT' WHERE id='rack_pull_below_knee'",
            "trap_bar_deadlift" to "UPDATE exercise_definitions SET familyId='deadlift', familyName='Deadlift', implement='SPECIALTY_BAR' WHERE id='trap_bar_deadlift'",
            "military_press" to "UPDATE exercise_definitions SET familyId='overhead_press', familyName='Overhead Press', implement='BARBELL', stance='STANDING', isPrimaryVariant=1 WHERE id='military_press'",
            "shoulder_press_hammer_strength" to "UPDATE exercise_definitions SET familyId='overhead_press', familyName='Overhead Press', implement='PLATE_LOADED', stance='SEATED' WHERE id='shoulder_press_hammer_strength'",
            "seated_smith_overhead_press" to "UPDATE exercise_definitions SET familyId='overhead_press', familyName='Overhead Press', implement='SMITH', stance='SEATED' WHERE id='seated_smith_overhead_press'",
            "shoulder_press_dumbbell" to "UPDATE exercise_definitions SET familyId='overhead_press', familyName='Overhead Press', implement='DUMBBELL', stance='SEATED' WHERE id='shoulder_press_dumbbell'",
            "shoulder_press_kettlebell" to "UPDATE exercise_definitions SET familyId='overhead_press', familyName='Overhead Press', implement='KETTLEBELL', stance='STANDING' WHERE id='shoulder_press_kettlebell'",
            "lateral_raise" to "UPDATE exercise_definitions SET familyId='lateral_raise', familyName='Lateral Raise', implement='DUMBBELL', isPrimaryVariant=1 WHERE id='lateral_raise'",
            "lateral_raise_cable" to "UPDATE exercise_definitions SET familyId='lateral_raise', familyName='Lateral Raise', implement='CABLE' WHERE id='lateral_raise_cable'",
            "lateral_raise_kettlebell" to "UPDATE exercise_definitions SET familyId='lateral_raise', familyName='Lateral Raise', implement='KETTLEBELL' WHERE id='lateral_raise_kettlebell'",
            "rear_delt_fly_dumbbell" to "UPDATE exercise_definitions SET familyId='rear_delt_fly', familyName='Rear Delt Fly', implement='DUMBBELL' WHERE id='rear_delt_fly_dumbbell'",
            "rear_delt_fly_machine" to "UPDATE exercise_definitions SET familyId='rear_delt_fly', familyName='Rear Delt Fly', implement='CABLE' WHERE id='rear_delt_fly_machine'",
            "back_squat" to "UPDATE exercise_definitions SET familyId='squat', familyName='Squat', implement='BARBELL', stance='BACK', isPrimaryVariant=1 WHERE id='back_squat'",
            "hack_squat_machine" to "UPDATE exercise_definitions SET familyId='hack_squat', familyName='Hack Squat', implement='MACHINE' WHERE id='hack_squat_machine'",
            "belt_squat" to "UPDATE exercise_definitions SET familyId='belt_squat', familyName='Belt Squat', implement='MACHINE' WHERE id='belt_squat'",
            "pendulum_squat" to "UPDATE exercise_definitions SET familyId='pendulum_squat', familyName='Pendulum Squat', implement='MACHINE' WHERE id='pendulum_squat'",
            "front_squat" to "UPDATE exercise_definitions SET familyId='squat', familyName='Squat', implement='BARBELL', stance='FRONT' WHERE id='front_squat'",
            "goblet_squat" to "UPDATE exercise_definitions SET familyId='squat', familyName='Squat', implement='DUMBBELL', stance='GOBLET' WHERE id='goblet_squat'",
            "hack_squat_plate_loaded" to "UPDATE exercise_definitions SET familyId='hack_squat', familyName='Hack Squat', implement='PLATE_LOADED' WHERE id='hack_squat_plate_loaded'",
            "leg_extension_cable" to "UPDATE exercise_definitions SET familyId='leg_extension', familyName='Leg Extension', implement='CABLE' WHERE id='leg_extension_cable'",
            "leg_extension_plate_loaded" to "UPDATE exercise_definitions SET familyId='leg_extension', familyName='Leg Extension', implement='PLATE_LOADED' WHERE id='leg_extension_plate_loaded'",
            "leg_curl_cable" to "UPDATE exercise_definitions SET familyId='leg_curl', familyName='Leg Curl', implement='CABLE' WHERE id='leg_curl_cable'",
            "leg_curl_plate_loaded" to "UPDATE exercise_definitions SET familyId='leg_curl', familyName='Leg Curl', implement='PLATE_LOADED' WHERE id='leg_curl_plate_loaded'",
            "lunge_barbell" to "UPDATE exercise_definitions SET familyId='lunge', familyName='Lunge', implement='BARBELL' WHERE id='lunge_barbell'",
            "lunge_dumbbell" to "UPDATE exercise_definitions SET familyId='lunge', familyName='Lunge', implement='DUMBBELL' WHERE id='lunge_dumbbell'",
            "lunge_bodyweight" to "UPDATE exercise_definitions SET familyId='lunge', familyName='Lunge', implement='BODYWEIGHT' WHERE id='lunge_bodyweight'",
            "hip_thrust_barbell" to "UPDATE exercise_definitions SET familyId='hip_thrust', familyName='Hip Thrust', implement='BARBELL' WHERE id='hip_thrust_barbell'",
            "hip_thrust_plate_loaded" to "UPDATE exercise_definitions SET familyId='hip_thrust', familyName='Hip Thrust', implement='PLATE_LOADED' WHERE id='hip_thrust_plate_loaded'",
            "romanian_deadlift" to "UPDATE exercise_definitions SET familyId='romanian_deadlift', familyName='Romanian Deadlift', implement='BARBELL', isPrimaryVariant=1 WHERE id='romanian_deadlift'",
            "romanian_deadlift_dumbbell" to "UPDATE exercise_definitions SET familyId='romanian_deadlift', familyName='Romanian Deadlift', implement='DUMBBELL' WHERE id='romanian_deadlift_dumbbell'",
            "deadlift" to "UPDATE exercise_definitions SET familyId='deadlift', familyName='Deadlift', implement='BARBELL', isPrimaryVariant=1 WHERE id='deadlift'",
            "kettlebell_swings" to "UPDATE exercise_definitions SET familyId='kettlebell_swing', familyName='Kettlebell Swing', implement='KETTLEBELL' WHERE id='kettlebell_swings'",
            "calf_raise_bodyweight" to "UPDATE exercise_definitions SET familyId='calf_raise', familyName='Calf Raise', implement='BODYWEIGHT' WHERE id='calf_raise_bodyweight'",
            "calf_raise" to "UPDATE exercise_definitions SET familyId='calf_raise', familyName='Calf Raise', implement='PLATE_LOADED', isPrimaryVariant=1 WHERE id='calf_raise'",
            "bicep_curl_barbell" to "UPDATE exercise_definitions SET familyId='bicep_curl', familyName='Bicep Curl', implement='BARBELL' WHERE id='bicep_curl_barbell'",
            "bicep_curl_dumbbell" to "UPDATE exercise_definitions SET familyId='bicep_curl', familyName='Bicep Curl', implement='DUMBBELL' WHERE id='bicep_curl_dumbbell'",
            "preacher_curl_ezbar" to "UPDATE exercise_definitions SET familyId='preacher_curl', familyName='Preacher Curl', implement='EZ_BAR' WHERE id='preacher_curl_ezbar'",
            "jerry_curl" to "UPDATE exercise_definitions SET familyId='jerry_curl', familyName='Jerry Curl', implement='DUMBBELL', isPrimaryVariant=1 WHERE id='jerry_curl'",
            "jerry_curl_kettlebell" to "UPDATE exercise_definitions SET familyId='jerry_curl', familyName='Jerry Curl', implement='KETTLEBELL' WHERE id='jerry_curl_kettlebell'",
            "hammer_curl" to "UPDATE exercise_definitions SET familyId='hammer_curl', familyName='Hammer Curl', implement='DUMBBELL', isPrimaryVariant=1 WHERE id='hammer_curl'",
            "hammer_curl_kettlebell" to "UPDATE exercise_definitions SET familyId='hammer_curl', familyName='Hammer Curl', implement='KETTLEBELL' WHERE id='hammer_curl_kettlebell'",
            "tricep_pushdown_cable" to "UPDATE exercise_definitions SET familyId='tricep_pushdown', familyName='Tricep Pushdown', implement='CABLE' WHERE id='tricep_pushdown_cable'",
            "db_tricep_extension" to "UPDATE exercise_definitions SET familyId='tricep_extension', familyName='Tricep Extension', implement='DUMBBELL', stance='SINGLE_ARM', isPrimaryVariant=1 WHERE id='db_tricep_extension'",
            "skull_crusher" to "UPDATE exercise_definitions SET familyId='skull_crusher', familyName='Skull Crusher', implement='EZ_BAR' WHERE id='skull_crusher'",
            "close_grip_smith_press" to "UPDATE exercise_definitions SET familyId='close_grip_bench', familyName='Close Grip Bench', implement='SMITH', stance='CLOSE_GRIP' WHERE id='close_grip_smith_press'",
            "dip_bodyweight" to "UPDATE exercise_definitions SET familyId='dip', familyName='Dip', implement='BODYWEIGHT' WHERE id='dip_bodyweight'",
            "weighted_dip" to "UPDATE exercise_definitions SET familyId='dip', familyName='Dip', implement='BODYWEIGHT', allowsAddedLoad=1, isPrimaryVariant=1 WHERE id='weighted_dip'",
            "cable_crunch" to "UPDATE exercise_definitions SET familyId='cable_crunch', familyName='Cable Crunch', implement='CABLE' WHERE id='cable_crunch'",
            "hanging_knee_raise" to "UPDATE exercise_definitions SET familyId='hanging_leg_raise', familyName='Hanging Leg Raise', implement='BODYWEIGHT' WHERE id='hanging_knee_raise'",
            "cyber_cluster_squat" to "UPDATE exercise_definitions SET familyId='squat', familyName='Squat', implement='BARBELL', stance='BACK' WHERE id='cyber_cluster_squat'",
            "zercher_squat" to "UPDATE exercise_definitions SET familyId='zercher_squat', familyName='Zercher Squat', implement='BARBELL', stance='ZERCHER', isPrimaryVariant=1 WHERE id='zercher_squat'"
        )

        for (update in updates.values) {
            db.execSQL(update)
        }

        // 3. Set defaults for any remaining rows (user custom exercises)
        db.execSQL("UPDATE exercise_definitions SET familyId = id, familyName = name, implement = 'OTHER' WHERE familyId = ''")
    }
}
