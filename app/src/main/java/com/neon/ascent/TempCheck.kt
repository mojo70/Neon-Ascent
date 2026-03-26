package com.neon.ascent

import androidx.health.connect.client.PermissionController

class TempCheck {
    fun check() {
        val contract = PermissionController.createPermissionResultContract()
    }
}
