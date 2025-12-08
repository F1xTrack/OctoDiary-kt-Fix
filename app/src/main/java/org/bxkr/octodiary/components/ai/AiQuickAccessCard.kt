package org.bxkr.octodiary.components.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.bxkr.octodiary.R
import org.bxkr.octodiary.Screen
import org.bxkr.octodiary.navControllerLive

/**
 * Карточка быстрого доступа к AI фичам на главном экране
 */
@Composable
fun AiQuickAccessCard() {
    val context = LocalContext.current
    val nav by navControllerLive.collectAsState()
    
    // Проверяем, включён ли AI
    val aiPrefs = context.getSharedPreferences("main_prefs", android.content.Context.MODE_PRIVATE)
    val aiEnabled = aiPrefs.getBoolean("ai_enabled", true)
    
    if (!aiEnabled) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.ai_helper),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = { nav?.navigate(Screen.AiDashboard.route) }) {
                    Text(stringResource(R.string.open))
                }
            }
            
            Text(
                stringResource(R.string.ai_features_description),
                style = MaterialTheme.typography.bodySmall, // Back to small
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 3, // Allow 3 lines
                overflow = TextOverflow.Ellipsis
            )
            
            HorizontalDivider()
            
            // Быстрые действия
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { nav?.navigate(Screen.VocabularySmartScreen.route) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Book, null, Modifier.size(20.dp))
                        Text(stringResource(R.string.smart_vocabulary), style = MaterialTheme.typography.labelSmall)
                    }
                }
                FilledTonalButton(
                    onClick = { nav?.navigate(Screen.LectureNotesScreen.route) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Description, null, Modifier.size(20.dp)) // Changed to Description icon
                        Text(stringResource(R.string.lecture_notes), style = MaterialTheme.typography.labelSmall)
                    }
                }
                FilledTonalButton(
                    onClick = { nav?.navigate(Screen.AiDashboard.route) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Dashboard, null, Modifier.size(20.dp))
                        Text(stringResource(R.string.ai_dashboard), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
