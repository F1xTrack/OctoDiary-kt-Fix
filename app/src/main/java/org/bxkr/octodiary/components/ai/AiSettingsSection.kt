package org.bxkr.octodiary.components.ai

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.bxkr.octodiary.R
import org.bxkr.octodiary.ai.GeminiService

/**
 * Секция настроек AI в главных настройках
 */
@Composable
fun AiSettingsSection() {
    val context = LocalContext.current
    
    var apiKey by remember { mutableStateOf(GeminiService.getApiKey(context)) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var bedTime by remember {
        val prefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("bed_time", "21:00") ?: "21:00")
    }
    var autoRecordLectures by remember {
        val prefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        mutableStateOf(prefs.getBoolean("auto_record_lectures", false))
    }
    
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.ai_helper),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // API ключ
        ListItem(
            headlineContent = { Text(stringResource(R.string.gemini_api_key)) },
            supportingContent = {
                Text(
                    if (apiKey.isEmpty()) stringResource(R.string.api_key_not_set) else stringResource(R.string.api_key_set)
                )
            },
            leadingContent = {
                Icon(Icons.Rounded.Key, null)
            },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = { showApiKeyDialog = true }) {
                    Text(stringResource(R.string.change))
                }
            }
        )
        
        HorizontalDivider()
        
        // Время сна
        ListItem(
            headlineContent = { Text(stringResource(R.string.bed_time)) },
            supportingContent = { Text(stringResource(R.string.bed_time_desc)) },
            leadingContent = {
                Icon(Icons.Rounded.Bedtime, null)
            },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                Text(bedTime, style = MaterialTheme.typography.bodyLarge)
            }
        )
        
        HorizontalDivider()
        
        // Автозапись лекций
        ListItem(
            headlineContent = { Text(stringResource(R.string.auto_record_lectures)) },
            supportingContent = { Text(stringResource(R.string.auto_record_desc)) },
            leadingContent = {
                Icon(Icons.Rounded.Mic, null)
            },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                Switch(
                    checked = autoRecordLectures,
                    onCheckedChange = {
                        autoRecordLectures = it
                        val prefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("auto_record_lectures", it).apply()

                        // Управляем сервисом при изменении настройки
                        if (it) {
                            org.bxkr.octodiary.audio.AutomaticLectureRecordingService.startAutomaticRecording(context)
                        } else {
                            org.bxkr.octodiary.audio.AutomaticLectureRecordingService.stopAutomaticRecording(context)
                        }
                    }
                )
            }
        )

        // Дополнительные настройки автозаписи
        if (autoRecordLectures) {
            val batteryLevel by remember {
                val prefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                mutableStateOf(prefs.getInt("auto_record_min_battery", 20))
            }
            val requireCharging by remember {
                val prefs = context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                mutableStateOf(prefs.getBoolean("auto_record_require_charging", false))
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.min_battery)) },
                supportingContent = { Text("${batteryLevel}%") },
                leadingContent = {
                    Icon(Icons.Rounded.BatteryStd, null)
                },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    var showBatteryDialog by remember { mutableStateOf(false) }
                    TextButton(onClick = { showBatteryDialog = true }) {
                        Text(stringResource(R.string.change))
                    }

                    if (showBatteryDialog) {
                        var newBatteryLevel by remember { mutableStateOf(batteryLevel.toString()) }
                        AlertDialog(
                            onDismissRequest = { showBatteryDialog = false },
                            title = { Text(stringResource(R.string.min_battery)) },
                            text = {
                                OutlinedTextField(
                                    value = newBatteryLevel,
                                    onValueChange = {
                                        newBatteryLevel = it.filter { char -> char.isDigit() }
                                    },
                                    label = { Text(stringResource(R.string.percent_hint)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val level = newBatteryLevel.toIntOrNull()?.coerceIn(10, 100) ?: 20
                                    context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putInt("auto_record_min_battery", level).apply()
                                    showBatteryDialog = false
                                }) {
                                    Text(stringResource(R.string.save))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBatteryDialog = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.charging_only)) },
                supportingContent = { Text(stringResource(R.string.charging_only_desc)) },
                leadingContent = {
                    Icon(Icons.Rounded.Power, null)
                },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    Switch(
                        checked = requireCharging,
                        onCheckedChange = {
                            context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("auto_record_require_charging", it).apply()
                        }
                    )
                }
            )
        }
    }
    
    // Диалог изменения API ключа
    if (showApiKeyDialog) {
        var newKey by remember { mutableStateOf(apiKey) }
        
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.gemini_api_key)) },
            text = {
                Column {
                    Text(stringResource(R.string.get_free_key))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text(stringResource(R.string.gemini_api_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    GeminiService.setApiKey(context, newKey)
                    apiKey = newKey
                    showApiKeyDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
