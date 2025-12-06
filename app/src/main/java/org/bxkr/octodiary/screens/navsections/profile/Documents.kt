package org.bxkr.octodiary.screens.navsections.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.R
import org.bxkr.octodiary.models.persondata.Document
import org.bxkr.octodiary.ui.theme.enterTransition
import org.bxkr.octodiary.ui.theme.exitTransition

@Composable
fun Documents() {
    val personDataState by DataService.personData.collectAsState(initial = null)
    val personData = personDataState
    val profile by DataService.profile.collectAsState()
    val currentProfile by DataService.currentProfile.collectAsState()

    if (personData == null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        val clipboardManager = LocalClipboardManager.current
        with(personData) {
            Text(stringResource(R.string.documents), style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                item {
                    Row {
                        Text(stringResource(R.string.snils_t))
                        val snils = profile?.children?.get(currentProfile)?.snils ?: ""
                        Text(
                            snils,
                            Modifier.clickable { clipboardManager.setText(AnnotatedString(snils)) })
                    }
                }
                items(items = documents) { document ->
                    DocumentCard(document = document)
                }
            }
        }
    }
}

@Composable
fun DocumentCard(document: Document) {
    var isExpanded by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val rotationAnim by
    animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(600),
        label = "rotate_anim"
    )

    val clipboardManager = LocalClipboardManager.current
    val copy: (String) -> Unit = remember {
        { clipboardManager.setText(AnnotatedString(it)) }
    }

    Card(
        Modifier
            .padding(top = 16.dp)
            .clip(CardDefaults.shape)
            .clickable {
                isExpanded = !isExpanded
                rotation += 180f
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                document.documentType.name,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                Icons.Rounded.ArrowDropDown,
                stringResource(R.string.expand),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .rotate(rotationAnim)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        AnimatedVisibility(
            isExpanded, exit = exitTransition, enter = enterTransition
        ) {
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth()
            ) {
                document.displayData.forEach {
                    if (it.value != null) {
                        NameAndValueText(stringResource(it.key), it.value!!) { copy(it.value!!) }
                    }
                }
            }
        }
    }
}