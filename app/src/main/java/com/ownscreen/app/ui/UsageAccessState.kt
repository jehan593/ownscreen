package com.ownscreen.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ownscreen.app.data.usage.UsageStatsPermissionHelper

/**
 * Usage Access is a special app-op permission with no runtime-permission callback, so the only
 * way to notice it was granted is to re-check when the user comes back to the app (they grant it
 * in a separate system Settings screen). Re-checks on every ON_RESUME.
 */
@Composable
fun rememberUsageAccessGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(UsageStatsPermissionHelper.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = UsageStatsPermissionHelper.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}
