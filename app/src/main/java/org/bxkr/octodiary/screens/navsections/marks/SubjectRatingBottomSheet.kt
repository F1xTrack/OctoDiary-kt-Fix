package org.bxkr.octodiary.screens.navsections.marks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.util.fastJoinToString
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.viewmodels.MarksViewModel
import org.bxkr.octodiary.viewmodels.MarksViewModelFactory
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.R
import org.bxkr.octodiary.components.ErrorMessage
import org.bxkr.octodiary.components.RankingMemberCard
import org.bxkr.octodiary.getDemoProperty
import org.bxkr.octodiary.isDemo
import org.bxkr.octodiary.models.rankingforsubject.RankingForSubject

@Composable
fun SubjectRatingBottomSheet(subjectId: Long, subjectName: String) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: MarksViewModel = viewModel(
        factory = MarksViewModelFactory(application, DataService)
    )
    val classMembers by viewModel.classMembers.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currentProfileIndex by viewModel.currentProfileIndex.collectAsState()

    var ranking by remember { mutableStateOf<List<RankingForSubject>?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (context.isDemo) {
            ranking =
                context.getDemoProperty<List<RankingForSubject>>(R.raw.demo_ranking_for_subject)
        } else {
            DataService.getRankingForSubject(subjectId, { errorText = it }) { ranking = it }
        }
    }

    Box(
        Modifier
            .heightIn(192.dp, Int.MAX_VALUE.dp)
            .fillMaxWidth()
    ) {
        if (ranking != null) {
            LazyColumn(Modifier.padding(8.dp)) {
                item {
                    Text(
                        subjectName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.based_on_current_marks),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .alpha(.8f)
                    )
                }
                items(ranking!!) {
                    val memberName = remember {
                        classMembers.firstOrNull { classMember ->
                            it.personId == classMember.personId
                        }?.user?.run {
                            listOf(
                                lastName,
                                firstName,
                                middleName ?: ""
                            ).fastJoinToString(" ")
                        }

                    }

                    RankingMemberCard(
                        rankPlace = it.rank.rankPlace,
                        average = it.rank.averageMarkFive,
                        memberName = memberName ?: it.personId,
                        highlighted = it.personId == profile?.children?.get(currentProfileIndex)?.contingentGuid,
                        isAnonymized = memberName == null
                    )
                }
            }
        } else if (errorText != null) {
            ErrorMessage(Modifier.align(Alignment.Center), errorText!!)
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}