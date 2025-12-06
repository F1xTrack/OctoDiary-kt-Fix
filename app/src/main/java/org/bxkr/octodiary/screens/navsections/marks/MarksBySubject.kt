package org.bxkr.octodiary.screens.navsections.marks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.viewmodels.MarksViewModel
import org.bxkr.octodiary.viewmodels.MarksViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.bxkr.octodiary.R
import org.bxkr.octodiary.contentDependentActionLive
import org.bxkr.octodiary.get
import org.bxkr.octodiary.getMarkConfig
import org.bxkr.octodiary.mainPrefs
import org.bxkr.octodiary.parseFromDay
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksBySubject(scrollToSubjectId: Long? = null) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: MarksViewModel = viewModel(
        factory = MarksViewModelFactory(application, DataService)
    )
    val marksSubject by viewModel.marksSubject.collectAsState()
    val subjectRanking by viewModel.subjectRanking.collectAsState()

    val filterState = remember { mutableStateOf(SubjectMarkFilterType.ByAverage) }
    contentDependentActionLive.value = { SubjectMarkFilter(state = filterState) }
    val periods = remember {
        val maxSubject = marksSubject.maxByOrNull {
            it.periods?.size ?: 0
        }
        maxSubject?.periods?.map { it.title to (it.startIso to it.endIso) }
    }
    var currentPeriod by remember {
        mutableStateOf(
            periods?.firstOrNull { (it.second.first.parseFromDay().time < Date().time) and (it.second.second.parseFromDay().time > Date().time) }?.first
                ?: periods?.firstOrNull()?.first
        )
    }
    var finalSelected by remember { mutableStateOf(false) }
    val markConfig = getMarkConfig()
    if (periods?.size?.let { it > 0 } == true) {
        Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
            Crossfade(targetState = finalSelected, modifier = Modifier.weight(1f)) {
                if (!it) {
                    Crossfade(
                        targetState = currentPeriod, label = "subject_anim"
                    ) { periodState ->
                        AnimatedContent(
                            targetState = filterState.value,
                            label = "filter_anim"
                        ) { filter ->
                            Column {
                                val filteredSubjects = marksSubject.filter {
                                    it.periods != null && it.periods.map { p -> p.title }.contains(periodState)
                                }
                                val subjects = when (filter) {
                                    SubjectMarkFilterType.Alphabetical -> filteredSubjects.sortedBy { it.subjectName }
                                    SubjectMarkFilterType.ByAverage -> filteredSubjects.sortedByDescending { it.periods?.first { p -> p.title == periodState }?.value?.toDoubleOrNull() }
                                    SubjectMarkFilterType.ByRanking -> filteredSubjects.sortedBy { subject ->
                                        subjectRanking.firstOrNull { r -> r.subjectId == subject.subjectId }?.rank?.rankPlace
                                    }
                                    SubjectMarkFilterType.ByUpdated -> filteredSubjects.sortedByDescending {
                                        it.periods?.first { p -> p.title == periodState }?.marks?.maxByOrNull { m ->
                                            m.date.parseFromDay().toInstant().toEpochMilli()
                                        }?.date?.parseFromDay()?.toInstant()?.toEpochMilli() ?: 0
                                    }
                                }
                                val lazyColumnState = rememberLazyListState()
                                val context = LocalContext.current
                                LazyColumn(
                                    Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 16.dp)
                                        .weight(1f),
                                    lazyColumnState
                                ) {
                                    val helpingIndex = (0..3).random()
                                    val showHints =
                                        context.mainPrefs.get<Boolean>("show_calc_hint") ?: true
                                    itemsIndexed(subjects) { index, subject ->
                                        if (subject.periods != null && subject.periods.any { p -> p.title == periodState }) {
                                            val sentPeriod =
                                                subject.periods.first { p -> p.title == periodState }
                                            SubjectCard(
                                                period = sentPeriod,
                                                subjectId = subject.subjectId,
                                                subjectName = subject.subjectName,
                                                showRating = sentPeriod == subject.currentPeriod,
                                                markConfig = markConfig,
                                                showHintOnce = (index == helpingIndex) && showHints
                                            )
                                        }
                                    }
                                }
                                if (scrollToSubjectId != null) {
                                    LaunchedEffect(Unit) {
                                        coroutineScope {
                                            lazyColumnState.animateScrollToItem(subjects.indexOfFirst { it.subjectId == scrollToSubjectId })
                                            scrollToSubjectIdLive.value = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    FinalsScreen()
                }
            }
            SecondaryScrollableTabRow(
                selectedTabIndex = if (!finalSelected) periods?.map { it.first }
                    ?.indexOf(currentPeriod) ?: 0 else periods?.map { it.first }?.size
                    ?: 0,
                divider = {},
                edgePadding = 0.dp
            ) {
                periods?.forEachIndexed { index: Int, period: Pair<String, Pair<String, String>> ->
                    Tab(
                        selected = (periods.map { it.first }
                            .indexOf(currentPeriod) == index) && !finalSelected,
                        text = { Text(period.first) },
                        onClick = {
                            currentPeriod = periods[index].first
                            finalSelected = false
                        })
                }
                Tab(
                    selected = finalSelected,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Star,
                                stringResource(R.string.finals),
                                Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp),
                                MaterialTheme.colorScheme.secondary
                            )
                            Text(stringResource(R.string.finals))
                        }
                    },
                    onClick = { finalSelected = true }
                )
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.no_periods_yet),
                Modifier.alpha(.8f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}