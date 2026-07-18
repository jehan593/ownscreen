package com.ownscreen.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.ui.components.AppIcon
import com.ownscreen.app.ui.rememberAppContainer
import com.ownscreen.app.ui.theme.nord11
import com.ownscreen.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDayScreen(epochDay: Long, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: HistoryDayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HistoryDayViewModel(
                    epochDay,
                    container.usageHistoryRepository,
                    container.installedAppsRepository,
                    container.suspendStateRepository
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.dayLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = "Total", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = TimeUtils.formatHoursMinutes(uiState.totalMinutes),
                    style = MaterialTheme.typography.displaySmall
                )
            }
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(icon = app.icon)
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )
                        if (app.isSuspended) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Blocked",
                                tint = nord11,
                                modifier = Modifier.padding(end = 8.dp).width(14.dp)
                            )
                        }
                        Text(text = TimeUtils.formatHoursMinutes(app.usedMinutes), style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
