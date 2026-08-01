package com.ownscreen.app.ui.modes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ownscreen.app.ui.components.MathChallengeDialog
import com.ownscreen.app.ui.rememberAppContainer
import com.ownscreen.app.ui.theme.nord11

/**
 * Lists every Mode with the currently active one marked. Tapping an inactive mode's row
 * activates it directly, unless the currently active mode has its "require trivia" flag set (see
 * [ModeDetailScreen]), in which case switching away from it is gated behind a math challenge
 * first. The edit icon opens [ModeDetailScreen] to rename a mode / change its app set without
 * switching to it; the Default mode has no edit affordance since it always has an empty, fixed
 * app set and can never require trivia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(onBack: () -> Unit, onOpenModeDetail: (Long) -> Unit, onCreateMode: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: ModesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ModesViewModel(container.modeRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var pendingActivation by remember { mutableStateOf<Long?>(null) }
    val activeMode = uiState.modes.find { it.id == uiState.activeModeId }

    pendingActivation?.let { targetModeId ->
        MathChallengeDialog(
            title = "Solve to switch modes",
            onSolved = {
                viewModel.activateMode(targetModeId)
                pendingActivation = null
            },
            onDismiss = { pendingActivation = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateMode) {
                        Icon(Icons.Filled.Add, contentDescription = "New mode")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(uiState.modes, key = { it.id }) { mode ->
                val isActive = mode.id == uiState.activeModeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isActive) {
                                if (activeMode?.requireTrivia == true) pendingActivation = mode.id
                                else viewModel.activateMode(mode.id)
                            } else if (!mode.isDefault) {
                                onOpenModeDetail(mode.id)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Active",
                            tint = nord11
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape)
                        )
                    }
                    Text(
                        text = mode.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp).weight(1f)
                    )
                    if (!mode.isDefault) {
                        IconButton(onClick = { onOpenModeDetail(mode.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ${mode.name}")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
