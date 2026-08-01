package com.ownscreen.app.ui.modes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.ui.components.AppIcon
import com.ownscreen.app.ui.components.MathChallengeDialog
import com.ownscreen.app.ui.rememberAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeDetailScreen(modeId: Long, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ModeDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ModeDetailViewModel(modeId, container.installedAppsRepository, container.modeRepository)
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    // Solving this once unlocks the rest of this screen visit — re-opening the screen later
    // (a fresh composition) asks again, since it's re-derived from uiState.originalRequireTrivia
    // each time rather than persisted anywhere.
    var entryChallengeSolved by remember { mutableStateOf(false) }
    val filteredApps = remember(searchQuery, uiState.apps) {
        if (searchQuery.isBlank()) uiState.apps else uiState.apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    fun doSave() {
        coroutineScope.launch {
            viewModel.save()
            onBack()
        }
    }

    fun doDelete() {
        coroutineScope.launch {
            viewModel.delete()
            onBack()
        }
    }

    // Gates opening the editor itself, not saving — solving it once at entry unlocks the rest of
    // this screen visit. Only applies to an existing mode that already had the flag on; a brand
    // new mode has nothing to protect yet. Delete lives behind this same gate (see the delete
    // IconButton below) rather than having its own trivia challenge, since reaching the delete
    // button already required solving it.
    val needsEntryChallenge = !uiState.isLoading && uiState.originalRequireTrivia && !entryChallengeSolved

    if (needsEntryChallenge) {
        MathChallengeDialog(
            title = "Solve to edit mode",
            onSolved = { entryChallengeSolved = true },
            onDismiss = onBack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New mode" else "Edit mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNew && !needsEntryChallenge) {
                        IconButton(onClick = { doDelete() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete mode")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || needsEntryChallenge) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
                label = { Text("Mode name") }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Require trivia", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Ask a math question when opening this mode to edit it, or before switching away from it while it's active.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = uiState.requireTrivia, onCheckedChange = viewModel::setRequireTrivia)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleApp(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(icon = app.icon)
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp).weight(1f)
                        )
                        Checkbox(
                            checked = app.isChecked,
                            onCheckedChange = { viewModel.toggleApp(app.packageName) }
                        )
                    }
                    HorizontalDivider()
                }
            }
            OutlinedButton(
                onClick = { doSave() },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Save")
            }
        }
    }
}
