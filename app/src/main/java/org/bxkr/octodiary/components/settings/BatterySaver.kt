package org.bxkr.octodiary.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.bxkr.octodiary.LocalActivity
import org.bxkr.octodiary.components.SwitchPreference
import org.bxkr.octodiary.get
import org.bxkr.octodiary.mainPrefs
import org.bxkr.octodiary.save
import org.bxkr.octodiary.utils.BatteryMonitor

@Composable
fun BatterySaverSettings() {
    val activity = LocalActivity.current
    
    // Включен ли режим экономии
    val batterySaverEnabled = remember { 
        mutableStateOf(activity.mainPrefs.get<Boolean>("battery_saver_enabled") ?: true) 
    }
    
    // Порог включения (%)
    val batterySaverThreshold = remember { 
        mutableIntStateOf(activity.mainPrefs.get<Int>("battery_saver_threshold") ?: 15) 
    }
    
    // Упрощать анимации
    val reduceAnimations = remember { 
        mutableStateOf(activity.mainPrefs.get<Boolean>("battery_saver_reduce_animations") ?: true) 
    }
    
    // Принудительная тёмная тема
    val forceDarkTheme = remember { 
        mutableStateOf(activity.mainPrefs.get<Boolean>("battery_saver_force_dark") ?: true) 
    }
    
    // Уменьшить частоту синхронизации
    val reduceSyncFrequency = remember { 
        mutableStateOf(activity.mainPrefs.get<Boolean>("battery_saver_reduce_sync") ?: true) 
    }
    
    // Текущий уровень заряда
    val currentBatteryLevel = remember { BatteryMonitor.getCurrentBatteryLevel(activity) }
    val isCharging = remember { BatteryMonitor.isCharging(activity) }
    val isBatterySaverActive = remember { BatteryMonitor.isBatterySaverActive(activity) }
    
    Column(Modifier.padding(vertical = 8.dp)) {
        
        // Заголовок секции
        Text(
            stringResource(org.bxkr.octodiary.R.string.battery_saver),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Статус батареи
        Card(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isBatterySaverActive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    isCharging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(org.bxkr.octodiary.R.string.current_battery),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when {
                            isCharging -> stringResource(org.bxkr.octodiary.R.string.charging)
                            isBatterySaverActive -> stringResource(org.bxkr.octodiary.R.string.battery_saver_active)
                            else -> stringResource(org.bxkr.octodiary.R.string.normal_mode)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            isCharging -> Icons.Rounded.BatteryChargingFull
                            currentBatteryLevel > 20 -> Icons.Rounded.BatteryFull
                            else -> Icons.Rounded.BatteryAlert
                        },
                        contentDescription = null,
                        tint = when {
                            isBatterySaverActive -> MaterialTheme.colorScheme.error
                            isCharging -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$currentBatteryLevel%",
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            isBatterySaverActive -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        Divider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        // Включение режима экономии
        SwitchPreference(
            title = stringResource(org.bxkr.octodiary.R.string.auto_battery_saver),
            description = stringResource(org.bxkr.octodiary.R.string.auto_battery_saver_desc),
            listenState = batterySaverEnabled
        ) {
            batterySaverEnabled.value = it
            activity.mainPrefs.save("battery_saver_enabled" to it)
            
            if (!it) {
                // Отключаем активный режим
                activity.mainPrefs.save("battery_saver_active" to false)
            }
        }
        
        // Настройки порога
        AnimatedVisibility(visible = batterySaverEnabled.value) {
            Column {
                Divider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(org.bxkr.octodiary.R.string.threshold),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${batterySaverThreshold.intValue}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Slider(
                        value = batterySaverThreshold.intValue.toFloat(),
                        onValueChange = { batterySaverThreshold.intValue = it.toInt() },
                        onValueChangeFinished = {
                            activity.mainPrefs.save("battery_saver_threshold" to batterySaverThreshold.intValue)
                        },
                        valueRange = 1f..100f,
                        steps = 98,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Text(
                        stringResource(org.bxkr.octodiary.R.string.battery_saver_threshold_desc, batterySaverThreshold.intValue),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Divider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                
                // Что делает режим экономии
                Text(
                    stringResource(org.bxkr.octodiary.R.string.battery_saver_actions),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                SwitchPreference(
                    title = stringResource(org.bxkr.octodiary.R.string.reduce_animations),
                    description = stringResource(org.bxkr.octodiary.R.string.reduce_animations_desc),
                    listenState = reduceAnimations
                ) {
                    reduceAnimations.value = it
                    activity.mainPrefs.save("battery_saver_reduce_animations" to it)
                }
                
                SwitchPreference(
                    title = stringResource(org.bxkr.octodiary.R.string.force_dark_theme),
                    description = stringResource(org.bxkr.octodiary.R.string.force_dark_theme_desc),
                    listenState = forceDarkTheme
                ) {
                    forceDarkTheme.value = it
                    activity.mainPrefs.save("battery_saver_force_dark" to it)
                }
                
                SwitchPreference(
                    title = stringResource(org.bxkr.octodiary.R.string.reduce_sync),
                    description = stringResource(org.bxkr.octodiary.R.string.reduce_sync_desc),
                    listenState = reduceSyncFrequency
                ) {
                    reduceSyncFrequency.value = it
                    activity.mainPrefs.save("battery_saver_reduce_sync" to it)
                }

                Divider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                // Настройки автообновления для экономии энергии
                Text(
                    stringResource(org.bxkr.octodiary.R.string.auto_update),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Минимальный уровень батареи для автообновления
                val autoUpdateMinBattery = remember {
                    mutableIntStateOf(activity.mainPrefs.get<Int>("auto_update_min_battery") ?: 20)
                }

                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(org.bxkr.octodiary.R.string.min_battery),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${autoUpdateMinBattery.intValue}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = autoUpdateMinBattery.intValue.toFloat(),
                        onValueChange = { autoUpdateMinBattery.intValue = it.toInt() },
                        onValueChangeFinished = {
                            activity.mainPrefs.save("auto_update_min_battery" to autoUpdateMinBattery.intValue)
                        },
                        valueRange = 1f..100f,
                        steps = 98,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Только по Wi-Fi
                val autoUpdateWifiOnly = remember {
                    mutableStateOf(activity.mainPrefs.get<Boolean>("auto_update_wifi_only") ?: true)
                }

                SwitchPreference(
                    title = stringResource(org.bxkr.octodiary.R.string.wifi_only),
                    description = stringResource(org.bxkr.octodiary.R.string.wifi_only_desc),
                    listenState = autoUpdateWifiOnly
                ) {
                    autoUpdateWifiOnly.value = it
                    activity.mainPrefs.save("auto_update_wifi_only" to it)
                }

                // Только при зарядке
                val autoUpdateChargingOnly = remember {
                    mutableStateOf(activity.mainPrefs.get<Boolean>("auto_update_charging_only") ?: false)
                }

                SwitchPreference(
                    title = stringResource(org.bxkr.octodiary.R.string.charging_only),
                    description = stringResource(org.bxkr.octodiary.R.string.charging_only_desc),
                    listenState = autoUpdateChargingOnly
                ) {
                    autoUpdateChargingOnly.value = it
                    activity.mainPrefs.save("auto_update_charging_only" to it)
                }
            }
        }
        
        Divider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        // Информация
        Card(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        stringResource(org.bxkr.octodiary.R.string.about_battery_saver),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(org.bxkr.octodiary.R.string.about_battery_saver_desc, batterySaverThreshold.intValue + 5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
