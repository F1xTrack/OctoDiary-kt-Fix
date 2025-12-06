package org.bxkr.octodiary.screens.navsections.marks

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.contentDependentActionIconLive
import org.bxkr.octodiary.showFilterLive
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bxkr.octodiary.viewmodels.MarksViewModel
import org.bxkr.octodiary.viewmodels.MarksViewModelFactory

val scrollToSubjectIdLive = MutableStateFlow<Long?>(null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: MarksViewModel = viewModel(
        factory = MarksViewModelFactory(application, DataService)
    )

    val marksSubject by viewModel.marksSubject.collectAsState()

    val scrollToSubjectId by scrollToSubjectIdLive.collectAsState()
    showFilterLive.value = true
    contentDependentActionIconLive.value = Icons.AutoMirrored.Rounded.Sort
    var currentTab by remember { mutableStateOf(if (scrollToSubjectId != null) MarksScreenTab.BySubject else MarksScreenTab.ByDate) }
    Column {
        if (marksSubject.isNotEmpty()) {
            PrimaryTabRow(selectedTabIndex = currentTab.ordinal, divider = {}) {
                MarksScreenTab.values().forEach {
                    Tab(
                        selected = currentTab == it,
                        onClick = {
                            currentTab = it
                        },
                        text = { Text(stringResource(id = it.title)) },
                        icon = { Icon(it.icon, stringResource(it.title)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                    )
                }
            }
        }
        Crossfade(targetState = currentTab, label = "marks_tab_anim") {
            when (it) {
                MarksScreenTab.ByDate -> MarksByDate()
                MarksScreenTab.BySubject -> MarksBySubject(scrollToSubjectId)
            }
        }
    }
}
