package com.ownscreen.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.data.usage.UsageStatsPermissionHelper
import com.ownscreen.app.ui.rememberAppContainer
import com.ownscreen.app.ui.rememberBatteryOptimizationsIgnored
import com.ownscreen.app.ui.rememberUsageAccessGranted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAppList: () -> Unit) {
    val context = LocalContext.current
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.ownDroidController) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val usageAccessGranted = rememberUsageAccessGranted()
    val batteryOptimizationsIgnored = rememberBatteryOptimizationsIgnored()
    val gap = Modifier.height(12.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!uiState.ownDroidInstalled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OwnDroid isn't installed. Install and configure it as Device Owner before blocking will work — see README.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(gap)

            Text("OwnDroid API key", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.setApiKey(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste the key set in OwnDroid's settings") }
            )

            Spacer(gap)
            HorizontalDivider()
            Spacer(gap)

            Text("Tracking", style = MaterialTheme.typography.titleSmall)
            if (usageAccessGranted) {
                Text(
                    text = "Active — starts automatically whenever Usage Access is granted.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Grant Usage Access to start tracking screen time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    onClick = { context.startActivity(UsageStatsPermissionHelper.usageAccessSettingsIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Usage Access")
                }
            }

            Spacer(gap)
            HorizontalDivider()
            Spacer(gap)

            Text(
                text = if (batteryOptimizationsIgnored) {
                    "Battery optimization already ignored for OwnScreen."
                } else {
                    "Some devices aggressively kill background services. If tracking stops unexpectedly, exempt OwnScreen from battery optimization."
                },
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        // Some OEM ROMs don't support the direct request intent — fall
                        // back to the app's own settings page where battery options
                        // (if any) can be adjusted manually.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                enabled = !batteryOptimizationsIgnored,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (batteryOptimizationsIgnored) "Battery optimization ignored" else "Ignore battery optimizations")
            }

            Spacer(gap)
            HorizontalDivider()
            Spacer(gap)

            Text("Check interval: ${uiState.pollIntervalSeconds}s", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "How often the background tracker refreshes the notification, widget, " +
                    "and history — lower is more up-to-date, higher uses a little less battery.",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = uiState.pollIntervalSeconds.toFloat(),
                onValueChange = { viewModel.setPollIntervalSeconds(it.toInt()) },
                valueRange = 15f..300f
            )

            Spacer(gap)
            HorizontalDivider()
            Spacer(gap)

            OutlinedButton(onClick = onOpenAppList, modifier = Modifier.fillMaxWidth()) {
                Text("All apps")
            }
        }
    }
}
