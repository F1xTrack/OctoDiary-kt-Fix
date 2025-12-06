package org.bxkr.octodiary.screens.navsections.profile

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.launch
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bxkr.octodiary.CachePrefs
import org.bxkr.octodiary.CloverShape
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.R
import org.bxkr.octodiary.cachePrefs
import org.bxkr.octodiary.get
import org.bxkr.octodiary.isDemo
import org.bxkr.octodiary.mainPrefs
import org.bxkr.octodiary.modalDialogCloseListenerLive
import org.bxkr.octodiary.modalDialogContentLive
import org.bxkr.octodiary.modalDialogStateLive
import org.bxkr.octodiary.models.classmembers.Assignment
import org.bxkr.octodiary.models.classmembers.ClassMember
import org.bxkr.octodiary.models.classmembers.OctoClassMembers
import org.bxkr.octodiary.save
import org.bxkr.octodiary.screens.navsections.dashboard.RankingList
import org.bxkr.octodiary.viewmodels.ClassInfoViewModel
import org.bxkr.octodiary.viewmodels.ClassInfoViewModelFactory

private val infoRecomposeTrigger = MutableLiveData(false)

@Composable
fun ClassInfo() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ClassInfoViewModel = viewModel(
        factory = ClassInfoViewModelFactory(application, DataService)
    )
    val classMembers by viewModel.classMembers.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currentProfileIndex by viewModel.currentProfileIndex.collectAsState()

    val trigger by infoRecomposeTrigger.observeAsState()
    val rankingList by viewModel.ranking.collectAsState()
    var showRanking by remember { mutableStateOf(false) }
    val enterTransition1 = remember {
        slideInHorizontally(
            tween(200)
        ) { it }
    }
    val exitTransition1 = remember {
        slideOutHorizontally(
            tween(200)
        ) { it }
    }
    val enterTransition2 = remember {
        slideInHorizontally(
            tween(200)
        ) { -it }
    }
    val exitTransition2 = remember {
        slideOutHorizontally(
            tween(200)
        ) { -it }
    }
    Box(Modifier.animateContentSize()) {
        AnimatedVisibility(
            visible = showRanking, enter = enterTransition1, exit = exitTransition1
        ) {
            Column {
                IconButton(onClick = { showRanking = false }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(id = R.string.back))
                }
                RankingList()
            }
        }
        AnimatedVisibility(
            visible = !showRanking, enter = enterTransition2, exit = exitTransition2
        ) {
            Column(Modifier.padding(16.dp)) {
                key(trigger) {
                    with(viewModel) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    stringResource(
                                        R.string.class_t, profile?.children?.get(currentProfileIndex)?.className ?: ""
                                    ), style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    LocalContext.current.resources.getQuantityString(
                                        R.plurals.student_count,
                                        classMembers.size,
                                        classMembers.size
                                    )
                                )
                            }
                        }
                        Row(
                            Modifier
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.name_column),
                                Modifier.alpha(.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (LocalContext.current.mainPrefs.get<Boolean>("main_rating") != false) {
                                Text(
                                    stringResource(R.string.rating_column),
                                    Modifier.alpha(.8f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        LazyColumn(
                            Modifier.clip(MaterialTheme.shapes.large),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(classMembers.sortedBy { it.fio }) {
                                var showDropdown by remember { mutableStateOf(false) }
                                Box {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                                        ), shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { showDropdown = true }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    it.user.lastName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(it.user.run { "$firstName ${middleName ?: ""}" })
                                            }
                                            if (LocalContext.current.mainPrefs.get<Boolean>("main_rating") != false) {
                                                FilledIconButton(
                                                    { showRanking = true },
                                                    shape = CloverShape
                                                ) {
                                                    Text(
                                                        rankingList.firstOrNull { rankingMember -> it.personId == rankingMember.personId }?.rank?.rankPlace?.toString()
                                                            ?: "?",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val context = LocalContext.current
                                    DropdownMenu(
                                        showDropdown && LocalContext.current.mainPrefs.get<Boolean>(
                                            "main_rating"
                                        ) != false && !context.isDemo,
                                        { showDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            { Text(stringResource(R.string.assign_id)) },
                                            {
                                                modalDialogContentLive.value = {
                                                    AssignIdDialog(it, viewModel)
                                                }
                                                modalDialogCloseListenerLive.value = { }
                                                modalDialogStateLive.value = true
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignIdDialog(member: ClassMember, viewModel: ClassInfoViewModel) {
    var personId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val getCopiedString = { clipboardManager.getText()?.toString() ?: "" }

    AlertDialog(
        {
            modalDialogStateLive.value = false
            modalDialogContentLive.value = {}
        },
        confirmButton = {
            TextButton({
                if (personId.isNotBlank() && member.studentId != null) {
                    isLoading = true
                    viewModel.assignPersonId(
                        member.studentId,
                        personId,
                        context.cachePrefs
                    ) {
                        isLoading = false
                        modalDialogStateLive.value = false
                        modalDialogContentLive.value = {}
                    }
                }
            }, enabled = !isLoading) {
                Text(stringResource(R.string.assign))
            }
        },
        title = { Text(stringResource(R.string.assign_id)) },
        icon = {
            AnimatedVisibility(!isLoading) {
                val tooltipState = rememberTooltipState(isPersistent = true)
                val coroutineScope = rememberCoroutineScope()
                TooltipBox(
                    TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    {
                        PlainTooltip {
                            Text(
                                stringResource(
                                    R.string.assign_id_help_description,
                                    stringResource(DataService.subsystem.title)
                                )
                            )
                        }
                    },
                    tooltipState
                ) {
                    IconButton({
                        coroutineScope.launch {
                            tooltipState.show()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.HelpOutline,
                            stringResource(R.string.what_is_it),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            AnimatedVisibility(isLoading) {
                CircularProgressIndicator()
            }
        },
        text = {
            Column {
                Text(member.fio)
                textField(personId, { personId = it }, R.string.student_id,
                    helpCopy = true,
                    onHelpCopyClick = { personId = getCopiedString() })
            }
        }
    )
}

@Composable
private fun textField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes hint: Int,
    helpCopy: Boolean = false,
    onHelpCopyClick: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next,
) = textField(
    value,
    onValueChange,
    stringResource(hint),
    helpCopy,
    onHelpCopyClick,
    imeAction
)

@Composable
private fun textField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    helpCopy: Boolean = false,
    onHelpCopyClick: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value,
        onValueChange,
        label = { Text(hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        trailingIcon = {
            if (helpCopy) IconButton(onClick = onHelpCopyClick) {
                Icon(
                    Icons.Rounded.ContentPaste,
                    stringResource(R.string.paste)
                )
            }
        }
    )
}

// Logic moved to ClassInfoViewModel