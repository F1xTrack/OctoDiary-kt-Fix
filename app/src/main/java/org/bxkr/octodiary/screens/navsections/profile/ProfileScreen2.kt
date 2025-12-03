package org.bxkr.octodiary.screens.navsections.profile

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import org.bxkr.octodiary.viewmodels.ProfileScreen2ViewModel
import org.bxkr.octodiary.viewmodels.ProfileScreen2ViewModelFactory
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.Diary
import org.bxkr.octodiary.R
import org.bxkr.octodiary.baseEnqueueOrNull
import org.bxkr.octodiary.launchPickerLive
import org.bxkr.octodiary.launchUrlLive
import org.bxkr.octodiary.modalBottomSheetContentLive
import org.bxkr.octodiary.modalBottomSheetStateLive
import org.bxkr.octodiary.modalDialogContentLive
import org.bxkr.octodiary.modalDialogStateLive
import org.bxkr.octodiary.models.profile.Children
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.models.avatar.Avatar
import org.bxkr.octodiary.models.govexams.GovExamsResponse
import kotlin.collections.firstOrNull
import kotlin.collections.isNullOrEmpty
import org.bxkr.octodiary.network.NetworkService
import org.bxkr.octodiary.screens.navsections.profile.meal.MealDialog



@Composable
fun ProfileScreen2() {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileScreen2ViewModel = viewModel(
        factory = ProfileScreen2ViewModelFactory(application, DataService)
    )

    val profileResponse by viewModel.profile.collectAsState()
    val avatars by viewModel.avatars.collectAsState()
    val govExams by viewModel.govExams.collectAsState()
    val subsystem by viewModel.subsystem.collectAsState()
    val currentProfileIndex by viewModel.currentProfileIndex.collectAsState()

    val child = profileResponse?.value?.children?.get(currentProfileIndex ?: 0)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (child != null && profileResponse != null) {
            ShortProfileInfo(child, profileResponse, avatars, viewModel)
            Cards(govExams, subsystem, viewModel)
        } else {
            // Show loading or error state
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ShortProfileInfo(child: Children, profileResponse: ProfileResponse, avatars: List<Avatar>?, viewModel: ProfileScreen2ViewModel) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 16.dp)) {
            val onAvatarClick = {
                modalDialogContentLive.value = {
                    var loading by remember { mutableStateOf(false) }
                    AlertDialog(
                        {
                            modalDialogStateLive.postValue(false)
                        },
                        confirmButton = {
                            TextButton(
                                { modalDialogStateLive.postValue(false) },
                                enabled = !loading
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                        title = { Text(stringResource(R.string.avatar)) },
                        text = {
                            Column {
                                AnimatedVisibility(loading) {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                AnimatedVisibility(!loading) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val actionCard =
                                            @Composable { onClick: () -> Unit, text: Int ->
                                                OutlinedCard(
                                                    onClick,
                                                    Modifier
                                                        .fillMaxWidth()
                                                ) {
                                                    Row(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(stringResource(text))
                                                    }
                                                }
                                            }
                                        actionCard(
                                            {
                                                loading = true
                                                launchPickerLive.value?.invoke()
                                            },
                                            R.string.choose_from_gallery
                                        )
                                        if (!avatars.isNullOrEmpty()) {
                                            actionCard({
                                                loading = true
                                                avatars.firstOrNull()?.let { avatar ->
                                                    viewModel.deleteAvatar(avatar.id.toString())
                                                }
                                            }, R.string.delete)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                modalDialogStateLive.postValue(true)
            }
            avatars?.firstOrNull()?.let { avatar ->
                GlideImage(
                    avatar.url.let { string -> Uri.parse(string) },
                    stringResource(R.string.avatar),
                    Modifier
                        .clip(CircleShape)
                        .clickable { onAvatarClick() }
                        .size(64.dp),
                    contentScale = ContentScale.Crop
                )
            } ?: Box( // Added a Box for the else branch when avatar is null or empty
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    )
            ) {
                Icon(
                    painterResource(R.drawable.ic_launcher_foreground),
                    stringResource(R.string.app_name),
                    Modifier.scale(1.4f),
                    MaterialTheme.colorScheme.run { secondary }
                )
            }
        }
        Column {
            Text(
                child.run { "$lastName $firstName $middleName" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(child.school.shortName)
            Text(stringResource(R.string.class_t, child.className))
        }
    }
}

@Composable
private fun Cards(govExams: GovExamsResponse?, subsystem: Diary?, viewModel: ProfileScreen2ViewModel) {
    val is77 = remember { subsystem == Diary.MES }
    Column(
        Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ProfileCard(R.string.personal_data, Icons.Rounded.Person, bottomSheetContent = { PersonalData() })
        ProfileCard(R.string.class_label, Icons.Rounded.Group, bottomSheetContent = { ClassInfo() })
        ProfileCard(R.string.school_and_teachers, Icons.Rounded.School, bottomSheetContent = { School() })
        if (!govExams?.data.isNullOrEmpty()) ProfileCard(
            R.string.exam_results,
            Icons.Rounded.Grade, bottomSheetContent = { ExamResults() })
        if (is77) ProfileCard(R.string.wallet, Icons.Rounded.Wallet, bottomSheetContent = { Wallet() })
        ProfileCard(R.string.meal, Icons.Rounded.Restaurant, onClick = viewModel::mealOnClick)
        ProfileCard(R.string.documents, Icons.Rounded.Description, bottomSheetContent = { Documents() })
    }
}



@Composable
private fun ProfileCard(
    @StringRes textRes: Int,
    icon: ImageVector,
    bottomSheetContent: @Composable () -> Unit,
) {
    ProfileCard(stringResource(textRes), icon, bottomSheetContent)
}

@Composable
private fun ProfileCard(
    @StringRes textRes: Int,
    icon: ImageVector,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    ProfileCard(stringResource(textRes), icon, onLongClick, onClick)
}

@Composable
private fun ProfileCard(
    text: String,
    icon: ImageVector,
    bottomSheetContent: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { openBottomSheet { bottomSheetContent() } }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.padding(end = 8.dp), MaterialTheme.colorScheme.onSurface)
        Text(text)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    text: String,
    icon: ImageVector,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clip(MaterialTheme.shapes.extraSmall)
            .combinedClickable(
                onLongClick = {
                    android.util.Log.d("ProfileCard", "Long click on card: $text")
                    onLongClick()
                },
                onClick = {
                    android.util.Log.d("ProfileCard", "Click on card: $text")
                    onClick()
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.padding(end = 8.dp), MaterialTheme.colorScheme.onSurface)
        Text(text)
    }
}



private fun openBottomSheet(content: @Composable () -> Unit) {
    modalBottomSheetStateLive.postValue(true)
    modalBottomSheetContentLive.postValue(content)
}