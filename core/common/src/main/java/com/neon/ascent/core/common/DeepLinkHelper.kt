package com.neon.ascent.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper to generate deep link intents for Neon Ascent.
 */
@Singleton
class DeepLinkHelper @Inject constructor(@ApplicationContext private val context: Context) {
    
    fun createDashboardIntent(): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("neon-ascent://dashboard"),
            context,
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.let { 
                Class.forName(it.className) 
            } ?: return Intent()
        )
    }

    fun createTaskCompletionIntent(taskId: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("neon-ascent://task/complete/$taskId")
        )
    }

    fun createBiohackingIntent(): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("neon-ascent://biohacking")
        )
    }

    fun createForgeIntent(attribute: String? = null, title: String? = null, description: String? = null): Intent {
        val uri = Uri.Builder()
            .scheme("neon-ascent")
            .authority("forge")
            .appendQueryParameter("attribute", attribute ?: "")
            .appendQueryParameter("title", title ?: "")
            .appendQueryParameter("description", description ?: "")
            .build()
        
        return Intent(
            Intent.ACTION_VIEW,
            uri,
            context,
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.let { 
                Class.forName(it.className) 
            } ?: return Intent()
        )
    }
}
