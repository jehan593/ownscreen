package com.ownscreen.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Repeats [action] every [intervalMillis], but only while this screen is actually RESUMED
 * (visible and in the foreground).
 *
 * Polling loops previously ran from `ViewModel.init { viewModelScope.launch { ... } }`, which
 * keeps going for the ViewModel's entire lifetime — including while a different screen is pushed
 * on top of it in the nav back stack (Compose Navigation only clears a ViewModel when its
 * back-stack entry is popped, not when it's merely covered), or while the whole app is
 * backgrounded. That silently burned CPU/battery re-querying UsageStatsManager for a screen
 * nobody was looking at. Driving the loop from here ties it to actual on-screen visibility.
 */
@Composable
fun LifecycleAwarePoll(intervalMillis: Long, action: suspend () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, intervalMillis) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                action()
                delay(intervalMillis)
            }
        }
    }
}
