package org.bxkr.octodiary.components.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.bxkr.octodiary.R
import org.bxkr.octodiary.ai.StreakManager

/**
 * Карточка стрика (серии дней)
 */
@Composable
fun StreakCard(type: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentStreak by remember { mutableStateOf(0) }
    var longestStreak by remember { mutableStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(type) {
        currentStreak = StreakManager.getCurrentStreak(context, type)
        longestStreak = StreakManager.getLongestStreak(context, type)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showInfoDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = if (currentStreak > 0) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    null,
                    Modifier.size(40.dp),
                    tint = if (currentStreak > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (currentStreak > 0) {
                            stringResource(R.string.streak_fire_format, currentStreak, getDayWord(context, currentStreak))
                        } else {
                            stringResource(R.string.streak_lost)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentStreak > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            if (longestStreak > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.longest_streak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$longestStreak",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Диалог с информацией о стрике
    if (showInfoDialog) {
        StreakInfoDialog(
            type = type,
            title = title,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreakInfoDialog(
    type: String,
    title: String,
    currentStreak: Int,
    longestStreak: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Настройки стрика
    val prefs = context.getSharedPreferences("streak_settings", android.content.Context.MODE_PRIVATE)
    var goalDays by remember { mutableStateOf(prefs.getInt("${type}_goal", 7)) }
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean("${type}_reminders", true)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            when (type) {
                "homework" -> Icon(Icons.Rounded.CheckCircle, null)
                "diary" -> Icon(Icons.Rounded.Book, null)
                "ai_usage" -> Icon(Icons.Rounded.AutoAwesome, null)
                else -> Icon(Icons.Rounded.LocalFireDepartment, null)
            }
        },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Объяснение стрика
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.what_is_it),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            getStreakExplanation(type, context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Divider()
                
                // Статистика
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$currentStreak",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.current_streak),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$longestStreak",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            stringResource(R.string.longest_streak),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Divider()
                
                // Настройки
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleSmall
                )
                
                // Цель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.goal_days))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            if (goalDays > 1) {
                                goalDays--
                                prefs.edit().putInt("${type}_goal", goalDays).apply()
                            }
                        }) {
                            Icon(Icons.Rounded.Remove, null)
                        }
                        Text(
                            "$goalDays",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = {
                            if (goalDays < 365) {
                                goalDays++
                                prefs.edit().putInt("${type}_goal", goalDays).apply()
                            }
                        }) {
                            Icon(Icons.Rounded.Add, null)
                        }
                    }
                }
                
                // Напоминания
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.reminders))
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            prefs.edit().putBoolean("${type}_reminders", it).apply()
                        }
                    )
                }
                
                // Прогресс к цели
                if (currentStreak > 0) {
                    Column {
                        Text(
                            stringResource(R.string.goal_progress, currentStreak, goalDays),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (currentStreak.toFloat() / goalDays.toFloat()).coerceAtMost(1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun getStreakExplanation(type: String, context: android.content.Context): String {
    return when (type) {
        "homework" -> context.getString(R.string.streak_homework_desc)
        "diary" -> context.getString(R.string.streak_diary_desc)
        "ai_usage" -> context.getString(R.string.streak_ai_usage_desc)
        else -> context.getString(R.string.streak_default_desc)
    }
}

private fun getDayWord(context: android.content.Context, count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    
    return when {
        lastTwoDigits in 11..19 -> context.getString(R.string.day_many)
        lastDigit == 1 -> context.getString(R.string.day_1)
        lastDigit in 2..4 -> context.getString(R.string.day_2_4)
        else -> context.getString(R.string.day_many)
    }
}
