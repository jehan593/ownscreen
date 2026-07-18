package com.ownscreen.app.enforcement

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Drives OwnDroid (com.bintianqi.owndroid) — a separately installed, offline Device-Owner
 * policy-controller app — through its intent-based broadcast API. We only use SUSPEND/UNSUSPEND:
 * the app icon stays visible, but launching it shows Android's built-in "app isn't available"
 * message until UNSUSPEND fires. This is fire-and-forget; OwnDroid's API gives no success
 * callback, so callers should gate on [isOwnDroidInstalled] and a non-blank API key.
 */
interface OwnDroidController {
    suspend fun suspend(packageName: String)
    suspend fun unsuspend(packageName: String)
    fun isOwnDroidInstalled(): Boolean
}

class OwnDroidBroadcastSender(
    private val context: Context,
    private val apiKeyProvider: suspend () -> String
) : OwnDroidController {

    companion object {
        const val OWNDROID_PACKAGE = "com.bintianqi.owndroid"
        const val OWNDROID_RECEIVER = "com.bintianqi.owndroid.ApiReceiver"
        const val ACTION_SUSPEND = "com.bintianqi.owndroid.action.SUSPEND"
        const val ACTION_UNSUSPEND = "com.bintianqi.owndroid.action.UNSUSPEND"
        const val EXTRA_KEY = "key"
        const val EXTRA_PACKAGE = "package"
    }

    override suspend fun suspend(packageName: String) = send(ACTION_SUSPEND, packageName)

    override suspend fun unsuspend(packageName: String) = send(ACTION_UNSUSPEND, packageName)

    private suspend fun send(action: String, packageName: String) {
        val key = apiKeyProvider()
        val intent = Intent(action).apply {
            component = ComponentName(OWNDROID_PACKAGE, OWNDROID_RECEIVER)
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_PACKAGE, packageName)
        }
        context.sendBroadcast(intent)
    }

    override fun isOwnDroidInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(OWNDROID_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
