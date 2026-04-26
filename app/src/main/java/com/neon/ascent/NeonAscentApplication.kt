package com.neon.ascent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class NeonAscentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
    }
}
