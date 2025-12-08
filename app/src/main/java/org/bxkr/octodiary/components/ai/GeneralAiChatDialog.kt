package org.bxkr.octodiary.components.ai

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import org.bxkr.octodiary.R

/**
 * Вкладка чата
 */
data class ChatTab(
    val id: String,
    val name: String
)

/**
 * Диалог общего AI помощника с вкладками
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GeneralAiChatDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Загружаем вкладки
    var tabs by remember { mutableStateOf<List<ChatTab>>(loadChatTabs(context)) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renamingTab by remember { mutableStateOf<ChatTab?>(null) }
    var showTabSettingsDialog by remember { mutableStateOf(false) }
    var selectedTabForSettings by remember { mutableStateOf<ChatTab?>(null) }

    // Логи для отслеживания изменений состояния
    LaunchedEffect(tabs) {
        Log.d("GeneralAiChatDialog", "Tabs changed: size=${tabs.size}, content=${tabs.map { it.name }}")
    }

    LaunchedEffect(selectedTabIndex) {
        Log.d("GeneralAiChatDialog", "selectedTabIndex changed to: $selectedTabIndex")
        // Проверяем валидность индекса
        if (selectedTabIndex >= tabs.size || selectedTabIndex < 0) {
            Log.w("GeneralAiChatDialog", "Invalid selectedTabIndex: $selectedTabIndex, tabs.size=${tabs.size}")
        }
    }
    
    // Кэшируем systemPrompt - иначе при рекомпозиции он меняется и ломает чат
    val systemPrompt = remember(context) {
        GeneralAiHelper.generateSystemPrompt(context)
    }
    
    val currentTab = tabs.getOrNull(selectedTabIndex) ?: tabs.firstOrNull()
    Log.d("GeneralAiChatDialog", "Current tab: ${currentTab?.name ?: "null"}, selectedTabIndex=$selectedTabIndex, tabs.size=${tabs.size}")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.general_ai_assistant)) },
                    actions = {
                        // Кнопка новой вкладки
                        val newChatTitle = stringResource(R.string.chat_n, tabs.size + 1)
                        IconButton(onClick = {
                            Log.d("GeneralAiChatDialog", "Adding new tab")
                            val newTab = ChatTab(
                                id = UUID.randomUUID().toString(),
                                name = newChatTitle
                            )
                            tabs = tabs + newTab
                            saveChatTabs(context, tabs)
                            selectedTabIndex = tabs.size - 1
                            Log.d("GeneralAiChatDialog", "New tab added: ${newTab.name}, selectedTabIndex set to $selectedTabIndex")
                        }) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.new_tab))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.close))
                        }
                    }
                )
                
                // Скроллящиеся вкладки
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 8.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                   Log.d("GeneralAiChatDialog", "Rendering tabs: selectedTabIndex=$selectedTabIndex, tabs.size=${tabs.size}")
                   tabs.forEachIndexed { index, tab ->
                         Log.d("GeneralAiChatDialog", "Rendering tab: ${tab.name} (index=$index), selected=${selectedTabIndex == index}")
                         Tab(
                             selected = selectedTabIndex == index,
                             onClick = {
                                 Log.d("GeneralAiChatDialog", "Tab onClick: ${tab.name}, index: $index, current selectedTabIndex=$selectedTabIndex")
                                 selectedTabIndex = index
                                 Log.d("GeneralAiChatDialog", "selectedTabIndex updated to: $selectedTabIndex")
                             },
                             modifier = Modifier,
                             text = {
                                 Row(
                                     horizontalArrangement = Arrangement.spacedBy(4.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(
                                         tab.name,
                                         maxLines = 1
                                     )
                                     // Кнопка настроек вкладки
                                     IconButton(
                                         onClick = {
                                             Log.d("GeneralAiChatDialog", "Tab settings clicked for: ${tab.name}")
                                             selectedTabForSettings = tab
                                             showTabSettingsDialog = true
                                         },
                                         modifier = Modifier.size(20.dp)
                                     ) {
                                         Icon(
                                             Icons.Rounded.Settings,
                                             contentDescription = stringResource(R.string.tab_settings_desc),
                                             modifier = Modifier.size(16.dp)
                                         )
                                     }
                                 }
                             }
                         )
                            
                        }
                 }

                 // Диалог настроек вкладки
                 if (showTabSettingsDialog && selectedTabForSettings != null) {
                     val newChatTitle = stringResource(R.string.chat_n, tabs.size + 1)
                     val copySuffix = stringResource(R.string.copy_suffix)
                     TabSettingsDialog(
                         currentTab = selectedTabForSettings!!,
                         tabsCount = tabs.size,
                         onDismiss = {
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         },
                         onRename = { tab: ChatTab ->
                             renamingTab = tab
                             showRenameDialog = true
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         },
                         onDelete = { tab: ChatTab ->
                             val deletingIndex = tabs.indexOfFirst { it.id == tab.id }
                             tabs = tabs.filter { it.id != tab.id }
                             saveChatTabs(context, tabs)
                             if (selectedTabIndex >= tabs.size) {
                                 selectedTabIndex = maxOf(0, tabs.size - 1)
                             }
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         },
                         onCreateNew = {
                             val newTab = ChatTab(
                                 id = UUID.randomUUID().toString(),
                                 name = newChatTitle
                             )
                             tabs = tabs + newTab
                             saveChatTabs(context, tabs)
                             selectedTabIndex = tabs.size - 1
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         },
                         onDuplicate = { tab: ChatTab ->
                             val duplicatedTab = ChatTab(
                                 id = UUID.randomUUID().toString(),
                                 name = "${tab.name}$copySuffix"
                             )
                             tabs = tabs + duplicatedTab
                             saveChatTabs(context, tabs)
                             selectedTabIndex = tabs.size - 1
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         },
                         onExport = { tab: ChatTab ->
                             // TODO: Реализовать экспорт истории чата
                             showTabSettingsDialog = false
                             selectedTabForSettings = null
                         }
                     )
                 }
             }
         }
     ) { padding ->
        if (currentTab != null) {
            AiChatComponent(
                chatId = "general_ai_${currentTab.id}",
                systemPrompt = systemPrompt,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Log.e("GeneralAiChatDialog", "No current tab available!")
            // Можно показать сообщение об ошибке или fallback UI
        }
    }

    // Диалог переименования
    if (showRenameDialog && renamingTab != null) {
        RenameTabDialog(
            currentName = renamingTab!!.name,
            onDismiss = {
                showRenameDialog = false
                renamingTab = null
            },
            onRename = { newName ->
                Log.d("GeneralAiChatDialog", "Renaming tab ${renamingTab?.name} to $newName")
                tabs = tabs.map {
                    if (it.id == renamingTab!!.id) it.copy(name = newName) else it
                }
                saveChatTabs(context, tabs)
                showRenameDialog = false
                renamingTab = null
            }
        )
    }
}

@Composable
fun TabSettingsDialog(
    currentTab: ChatTab?,
    tabsCount: Int,
    onDismiss: () -> Unit,
    onRename: (ChatTab) -> Unit,
    onDelete: (ChatTab) -> Unit,
    onCreateNew: () -> Unit,
    onDuplicate: (ChatTab) -> Unit,
    onExport: (ChatTab) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_tab_title, currentTab?.name ?: "")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentTab != null) {
                    OutlinedButton(
                        onClick = { onRename(currentTab) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.rename))
                        }
                    }

                    OutlinedButton(
                        onClick = { onDuplicate(currentTab) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.duplicate))
                        }
                    }

                    OutlinedButton(
                        onClick = { onExport(currentTab) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.export_history))
                        }
                    }

                    if (tabsCount > 1) {
                        OutlinedButton(
                            onClick = { onDelete(currentTab) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.delete))
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.new_tab))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun RenameTabDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_chat)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.name_field)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Сохранение и загрузка вкладок
private fun loadChatTabs(context: Context): List<ChatTab> {
    val prefs = context.getSharedPreferences("general_ai_tabs", Context.MODE_PRIVATE)
    val json = prefs.getString("tabs", null)
    Log.d("GeneralAiChatDialog", "Loading chat tabs from prefs: json=${json?.take(100)}...")

    return if (json != null) {
        try {
            val type = object : TypeToken<List<ChatTab>>() {}.type
            val loadedTabs = Gson().fromJson(json, type) ?: getDefaultTabs(context)
            Log.d("GeneralAiChatDialog", "Loaded tabs: ${loadedTabs.size} tabs")
            loadedTabs
        } catch (e: Exception) {
            Log.w("GeneralAiChatDialog", "Failed to load tabs, using defaults: ${e.message}")
            getDefaultTabs(context)
        }
    } else {
        Log.d("GeneralAiChatDialog", "No saved tabs, using defaults")
        getDefaultTabs(context)
    }
}

private fun saveChatTabs(context: Context, tabs: List<ChatTab>) {
    val prefs = context.getSharedPreferences("general_ai_tabs", Context.MODE_PRIVATE)
    val json = Gson().toJson(tabs)
    prefs.edit().putString("tabs", json).apply()
    Log.d("GeneralAiChatDialog", "Saved tabs to prefs: ${tabs.size} tabs, json length=${json.length}")
}

private fun getDefaultTabs(context: Context) = listOf(
    ChatTab("default", context.getString(R.string.main_chat))
)
