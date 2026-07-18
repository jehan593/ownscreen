package com.ownscreen.app.ui.appdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.ui.LifecycleAwarePoll
import com.ownscreen.app.ui.components.AppIcon
import com.ownscreen.app.ui.components.MathChallengeDialog
import com.ownscreen.app.ui.rememberAppContainer
import com.ownscreen.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: AppDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AppDetailViewModel(
                    packageName,
                    container.usageStatsRepository,
                    container.installedAppsRepository,
                    container.suspendStateRepository,
                    container.settingsRepository,
                    container.packageSuspensionChecker,
                    container.ownDroidController
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var showUnblockChallenge by remember { mutableStateOf(false) }

    LifecycleAwarePoll(AppDetailViewModel.POLL_INTERVAL_MILLIS) { viewModel.refreshUsageAndBlockStatus() }

    if (showUnblockChallenge) {
        MathChallengeDialog(
            onSolved = {
                viewModel.unblock()
                showUnblockChallenge = false
            },
            onDismiss = { showUnblockChallenge = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            uiState.icon?.let { AppIcon(icon = it, size = 56.dp) }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Used today", style = MaterialTheme.typography.labelMedium)
                    Text(TimeUtils.formatHoursMinutes(uiState.usedMinutes), style = MaterialTheme.typography.headlineMedium)
                    if (uiState.isSuspended) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Blocked via OwnDroid",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (uiState.blockLikelyIneffective) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This doesn't look like it actually took effect — the app " +
                                    "may still be usable. Double-check the OwnDroid API key in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isSuspended) {
                OutlinedButton(onClick = { showUnblockChallenge = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Unblock")
                }
            } else {
                if (!uiState.canBlock) {
                    Text(
                        text = if (!uiState.ownDroidInstalled) {
                            "OwnDroid isn't installed — blocking won't take effect. See Settings."
                        } else {
                            "No OwnDroid API key set — blocking won't take effect. See Settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = { viewModel.block() },
                    enabled = uiState.canBlock,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Block")
                }
            }
        }
    }
}
