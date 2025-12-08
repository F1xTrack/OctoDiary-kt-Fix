package org.bxkr.octodiary.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.ai.GeminiService
import org.bxkr.octodiary.ai.HomeworkAnalyzer
import org.bxkr.octodiary.components.ai.AutomaticRecordingStatusCard
import org.bxkr.octodiary.components.ai.GeneralAiChatDialog
import org.bxkr.octodiary.components.ai.PdfTextExtractorDialog
import org.bxkr.octodiary.components.ai.StreakCard
import org.bxkr.octodiary.components.ai.StudyPlanCard
import org.bxkr.octodiary.database.AppDatabase
import org.bxkr.octodiary.database.entity.KnowledgeBaseEntity
import org.bxkr.octodiary.models.ai.StudyPlan
import org.bxkr.octodiary.navControllerLive
import java.util.Date

/**
 * AI Дашборд - главный экран с аналитикой
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDashboardScreen() {
    val context = LocalContext.current
    val nav by navControllerLive.collectAsState()
    val scope = rememberCoroutineScope()
    
    var studyPlan by remember { mutableStateOf<StudyPlan?>(null) }
    var isGeneratingPlan by remember { mutableStateOf(false) }
    var apiKeySet by remember { mutableStateOf(GeminiService.getApiKey(context).isNotEmpty()) }
    var showGeneralAiChat by remember { mutableStateOf(false) }
    var showPdfExtractor by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.ai_helper)) },
                navigationIcon = {
                    IconButton(onClick = { nav?.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(Icons.Rounded.Settings, androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        if (!apiKeySet) {
            // Показать экран настройки API ключа
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Key,
                    null,
                    Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.configure_api_key),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.api_key_required),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                
                var apiKey by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.gemini_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        GeminiService.setApiKey(context, apiKey)
                        apiKeySet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.save))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.get_free_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Заголовок
                item {
                    Text(
                        androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.your_stats),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                // Стрики
                item {
                    Text(
                        androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.streaks),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                item {
                    StreakCard(
                        type = "homework",
                        title = androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.streak_homework),
                        icon = Icons.Rounded.CheckCircle
                    )
                }
                item {
                    StreakCard(
                        type = "grades",
                        title = androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.streak_grades),
                        icon = Icons.Rounded.Star
                    )
                }
                item {
                    StreakCard(
                        type = "study",
                        title = androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.streak_study),
                        icon = Icons.Rounded.MenuBook
                    )
                }
                
                // Персональный план
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.study_plan),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (DataService.homeworksFlow.value.isNotEmpty()) {
                            TextButton(onClick = {
                                isGeneratingPlan = true
                                scope.launch {
                                    val result = HomeworkAnalyzer.generateStudyPlan(
                                        context,
                                        DataService.homeworksFlow.value.filter { !it.isDone }
                                    )
                                    result.onSuccess { plan ->
                                        studyPlan = plan
                                    }
                                    isGeneratingPlan = false
                                }
                            }) {
                                Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.generate))
                            }
                        }
                    }
                }
                
                if (isGeneratingPlan) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                                Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.generating_plan))
                            }
                        }
                    }
                } else if (studyPlan != null) {
                    item {
                        StudyPlanCard(studyPlan!!)
                    }
                } else if (DataService.homeworksFlow.value.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.no_plan_today))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.ai_will_help),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // Автоматическая запись лекций
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.auto_record_lectures),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                item {
                    AutomaticRecordingStatusCard()
                }

                // Быстрые действия
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.quick_actions),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { /* Открыть словарь */ },
                            Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Book, null)
                            Spacer(Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.smart_vocabulary))
                        }
                        FilledTonalButton(
                            onClick = { /* Открыть конспекты */ },
                            Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Mic, null)
                            Spacer(Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.lecture_notes))
                        }
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { nav?.navigate(org.bxkr.octodiary.Screen.TextbookExtractorScreen.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.AutoStories, null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.extract_from_textbooks))
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { nav?.navigate(org.bxkr.octodiary.Screen.TextbooksScreen.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.textbook_library))
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { showPdfExtractor = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.extract_pdf))
                    }
                }
                
                // Широкая кнопка общего AI помощника
                item {
                    Button(
                        onClick = { showGeneralAiChat = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.SmartToy, null, Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            androidx.compose.ui.res.stringResource(org.bxkr.octodiary.R.string.general_ai_assistant),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
    
    // Диалог общего AI чата
    if (showGeneralAiChat) {
        GeneralAiChatDialog(
            onDismiss = { showGeneralAiChat = false }
        )
    }

    // Диалог извлечения текста из PDF
    if (showPdfExtractor) {
        PdfTextExtractorDialog(
            onDismiss = { showPdfExtractor = false },
            onTextExtracted = { extractedText ->
                // Сохраняем извлеченный текст в базу знаний
                scope.launch {
                    try {
                        val knowledgeBaseItem = KnowledgeBaseEntity(
                            subject = "PDF Import",
                            topic = "Извлеченный текст",
                            type = "note",
                            title = "Текст из PDF документа",
                            content = extractedText,
                            source = "pdf_import",
                            createdAt = Date(),
                            updatedAt = Date(),
                            searchableText = extractedText
                        )
 
                                        // Сохраняем в базу данных
                                        AppDatabase.getInstance(context)?.knowledgeBaseDao()?.insert(knowledgeBaseItem)
                        // Показываем уведомление об успехе
                        android.widget.Toast.makeText(
                            context,
                            context.getString(org.bxkr.octodiary.R.string.pdf_extracted_success),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
 
                    } catch (e: Exception) {
                        android.util.Log.e("PdfExtractor", "Error saving to database", e)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(org.bxkr.octodiary.R.string.pdf_extracted_error, e.message),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}
