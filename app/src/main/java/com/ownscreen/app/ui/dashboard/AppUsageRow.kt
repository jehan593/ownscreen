package com.ownscreen.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ownscreen.app.ui.components.AppIcon
import com.ownscreen.app.ui.theme.nord11
import com.ownscreen.app.util.TimeUtils

@Composable
fun AppUsageRow(app: AppUsageUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(icon = app.icon)
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = app.label, style = MaterialTheme.typography.titleSmall)
            if (app.isSuspended) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Blocked",
                    tint = nord11,
                    modifier = Modifier.width(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = TimeUtils.formatHoursMinutes(app.usedMinutes), style = MaterialTheme.typography.bodyMedium)
    }
}
