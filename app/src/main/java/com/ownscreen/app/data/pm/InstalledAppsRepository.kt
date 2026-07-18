package com.ownscreen.app.data.pm

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/**
 * The installed-app list is expensive to compute (PackageManager query + loadIcon() per app) but
 * rarely changes — most callers just want "what's installed," not a live-updating view of it. In
 * particular the widget re-queries this on every refresh tick (every 15-300s while a widget
 * instance exists), which without caching means a full PackageManager scan + icon load for every
 * installed app that often, just to compute a top-3 list. A short TTL cache cuts that down
 * without needing an install/uninstall broadcast receiver — a newly installed app just takes up
 * to [CACHE_TTL_MILLIS] to show up, which is an acceptable trade for how much repeated work it
 * avoids.
 */
class InstalledAppsRepository(private val packageManager: PackageManager, private val selfPackage: String) {

    private val mutex = Mutex()
    private var cache: List<LaunchableApp>? = null
    private var cachedAtMillis: Long = 0L

    suspend fun getLaunchableApps(): List<LaunchableApp> = mutex.withLock {
        val cached = cache
        val now = System.currentTimeMillis()
        if (cached != null && now - cachedAtMillis < CACHE_TTL_MILLIS) {
            return@withLock cached
        }
        val fresh = queryLaunchableApps()
        cache = fresh
        cachedAtMillis = now
        fresh
    }

    private suspend fun queryLaunchableApps(): List<LaunchableApp> = withContext(Dispatchers.Default) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != selfPackage }
            .map { appInfo: ApplicationInfo ->
                LaunchableApp(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    companion object {
        private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
