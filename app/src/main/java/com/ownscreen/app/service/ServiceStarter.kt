package com.ownscreen.app.service

import android.content.Context
import com.ownscreen.app.data.usage.UsageStatsPermissionHelper

/**
 * Single funnel for starting [UsageMonitorService] so [BootCompletedReceiver] and MainActivity's
 * create/resume checks all agree on the one real condition: Usage Access is granted. There's no
 * separate "tracking enabled" preference — once that permission is granted, tracking just runs.
 */
object ServiceStarter {

    fun ensureRunning(context: Context) {
        if (UsageStatsPermissionHelper.isGranted(context)) {
            UsageMonitorService.start(context)
        }
    }
}
