package com.ownscreen.app.data.pm

import android.content.pm.PackageManager

/**
 * Best-effort ground truth for whether Android actually has [packageName] suspended right now —
 * this is a real OS-level flag, set when OwnDroid (as device owner) genuinely suspends an app,
 * independent of whatever OwnDroid's fire-and-forget broadcast API tells (or doesn't tell) us.
 *
 * `PackageManager.isPackageSuspended(String)`'s public documentation focuses on launchers
 * checking suspended apps to gray out their icons; whether an arbitrary third-party app (not the
 * launcher, not the device owner) is permitted to call it on another app's package — versus
 * getting a SecurityException — isn't consistently documented and can't be confirmed without
 * running on a real device. This fails safe (returns null, "unknown") for any failure rather than
 * guessing either way, so callers must treat null as "couldn't verify," not as false.
 */
class PackageSuspensionChecker(private val packageManager: PackageManager) {

    fun isSuspendedOrNull(packageName: String): Boolean? = try {
        packageManager.isPackageSuspended(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: SecurityException) {
        null
    }
}
