package com.ownscreen.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ownscreen.app.widget.WidgetRefreshAlarmReceiver

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        ServiceStarter.ensureRunning(context)
        // setInexactRepeating alarms don't survive reboot; re-arm the fallback here since we
        // already hold RECEIVE_BOOT_COMPLETED for the line above.
        WidgetRefreshAlarmReceiver.schedule(context)
    }
}
