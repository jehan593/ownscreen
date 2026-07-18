package com.ownscreen.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.ownscreen.app.util.MathChallenge

/**
 * Deliberate friction gate before an unblock action goes through. Generates a fresh problem so it
 * can't be pre-memorized, and swaps in a new one on a wrong answer instead of letting the user
 * just keep guessing the same problem.
 */
@Composable
fun MathChallengeDialog(onSolved: () -> Unit, onDismiss: () -> Unit) {
    var challenge by remember { mutableStateOf(MathChallenge.random()) }
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solve to unblock") },
        text = {
            Column {
                Text("What is ${challenge.question}?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        showError = false
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Not quite — try this one instead") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (input.trim().toIntOrNull() == challenge.answer) {
                    onSolved()
                } else {
                    challenge = MathChallenge.random()
                    input = ""
                    showError = true
                }
            }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
