package com.ownscreen.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.data.usage.UsageStatsPermissionHelper
import com.ownscreen.app.ui.LifecycleAwarePoll
import com.ownscreen.app.ui.rememberAppContainer
import com.ownscreen.app.ui.rememberUsageAccessGranted
import com.ownscreen.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenSettings: () -> Unit, onOpenAppDetail: (String) -> Unit, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()

    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.usageStatsRepository,
                    container.installedAppsRepository,
                    container.suspendStateRepository
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val usageAccessGranted = rememberUsageAccessGranted()

    LifecycleAwarePoll(DashboardViewModel.REFRESH_INTERVAL_MILLIS) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OwnScreen") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.DateRange, contentDescription = "History")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!usageAccessGranted) {
                UsageAccessBanner(onGrant = { context.startActivity(UsageStatsPermissionHelper.usageAccessSettingsIntent()) })
            }

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Today", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = TimeUtils.formatHoursMinutes(uiState.totalMinutes),
                    style = MaterialTheme.typography.displaySmall
                )
            }
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    AppUsageRow(app = app, onClick = { onOpenAppDetail(app.packageName) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun UsageAccessBanner(onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Usage access required",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Grant OwnScreen usage-access permission to track screen time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Button(onClick = onGrant) { Text("Grant") }
    }
}
