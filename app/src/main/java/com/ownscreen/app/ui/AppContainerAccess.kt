package com.ownscreen.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ownscreen.app.OwnScreenApplication
import com.ownscreen.app.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    return remember { (context.applicationContext as OwnScreenApplication).container }
}
