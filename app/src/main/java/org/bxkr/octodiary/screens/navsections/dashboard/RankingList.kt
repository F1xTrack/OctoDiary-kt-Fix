package org.bxkr.octodiary.screens.navsections.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.components.RankingMemberCard

@Composable
fun RankingList() {
    val ranking by DataService.ranking.collectAsState()
    val profile by DataService.profile.collectAsState()
    val classMembers by DataService.classMembers.collectAsState()
    val currentProfile by DataService.currentProfile.collectAsState()
    
    LazyColumn(
        Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        items(ranking) { rankingMember ->
            val memberName = remember {
                val child = profile?.children?.get(currentProfile)
                if (rankingMember.personId != child?.contingentGuid) {
                    classMembers.firstOrNull { classMember ->
                        rankingMember.personId == classMember.personId
                    }?.fio
                } else child?.run {
                    listOf(
                        lastName,
                        firstName,
                        middleName
                    ).fastJoinToString(" ")
                }
            }
            RankingMemberCard(
                rankPlace = rankingMember.rank.rankPlace,
                average = rankingMember.rank.averageMarkFive,
                memberName = memberName ?: rankingMember.personId,
                highlighted = rankingMember.personId == profile?.children?.get(currentProfile)?.contingentGuid,
                isAnonymized = memberName == null
            )
        }
    }
}