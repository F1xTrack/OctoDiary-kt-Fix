package org.bxkr.octodiary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.Diary
import org.bxkr.octodiary.data.AuthRepository
import org.bxkr.octodiary.models.avatar.Avatar
import org.bxkr.octodiary.models.govexams.GovExamsResponse
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.modalDialogStateLive
import org.bxkr.octodiary.modalDialogContentLive
import org.bxkr.octodiary.screens.navsections.profile.meal.MealDialog
import org.bxkr.octodiary.launchUrlLive
import android.net.Uri
import org.bxkr.octodiary.network.NetworkService
import org.bxkr.octodiary.baseEnqueueOrNull
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.ContentResolver

// Factory for ProfileScreen2ViewModel
class ProfileScreen2ViewModelFactory(private val application: Application, private val dataService: DataService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileScreen2ViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileScreen2ViewModel(application, dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ProfileScreen2ViewModel(application: Application, private val dataService: DataService) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = dataService.authRepository

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile

    private val _avatars = MutableStateFlow<List<Avatar>?>(null)
    val avatars: StateFlow<List<Avatar>?> = _avatars

    private val _govExams = MutableStateFlow<GovExamsResponse?>(null)
    val govExams: StateFlow<GovExamsResponse?> = _govExams

    private val _subsystem = MutableStateFlow<Diary?>(null)
    val subsystem: StateFlow<Diary?> = _subsystem

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _currentProfileIndex = MutableStateFlow<Int>(0)
    val currentProfileIndex: StateFlow<Int> = _currentProfileIndex

    init {
        _currentProfileIndex.value = dataService.currentProfile

        loadProfileData()
        loadAvatars()
        loadGovExams()
        _subsystem.value = dataService.subsystem
        _token.value = dataService.token

        // Observe pickedImageUri for avatar upload
        viewModelScope.launch {
            dataService.pickedImageUri.observeForever { uri: Uri? ->
                uri?.let {
                    uploadAvatar(it, getApplication<Application>().contentResolver)
                    dataService.pickedImageUri.postValue(null) // Clear after processing
                }
            }
        }
    }

    fun loadProfileData() {
        viewModelScope.launch {
            dataService.updateProfile {
                _profile.value = dataService.profile
            }
        }
    }

    fun loadAvatars() {
        viewModelScope.launch {
            dataService.updateAvatars {
                _avatars.value = dataService.avatars
            }
        }
    }

    fun loadGovExams() {
        viewModelScope.launch {
            dataService.updateGovExams {
                _govExams.value = dataService.govExams
            }
        }
    }

    fun onAvatarClick() {
        modalDialogStateLive.postValue(true)
    }

    fun setCurrentProfileIndex(index: Int) {
        dataService.currentProfile = index
        _currentProfileIndex.value = index
        loadProfileData()
        loadAvatars()
        loadGovExams()
    }

    fun mealOnClick() {
        if (dataService.subsystem == Diary.MES) {
            modalDialogContentLive.value = { MealDialog() }
            modalDialogStateLive.postValue(true)
        } else {
            launchUrlLive.postValue(Uri.parse(NetworkService.MySchoolAPIConfig.FOOD_URI))
        }
    }

    fun deleteAvatar(avatarId: String) {
        viewModelScope.launch {
            dataService.secondaryApi.deleteAvatar(
                "Bearer ${dataService.token}",
                dataService.profile.children[dataService.currentProfile].contingentGuid,
                avatarId
            ).baseEnqueueOrNull {
                loadAvatars()
                modalDialogStateLive.postValue(false)
            }
        }
    }

    fun uploadAvatar(imageUri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val bitmap = contentResolver.query(imageUri, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                val string = cursor.getString(idx)
                BitmapFactory.decodeFile(string)
            } ?: return@launch

            val byteOutputStream = ByteArrayOutputStream()
            bitmap.run {
                if (height > width) {
                    scale(200, height / (width / 200))
                } else if (width > height) {
                    scale(width / (height / 200), 200)
                } else scale(200, 200)
            }.compress(Bitmap.CompressFormat.PNG, 100, byteOutputStream)
            val requestFile = RequestBody.create(
                "multipart/form-data".toMediaType(),
                byteOutputStream.toByteArray()
            )
            val part = MultipartBody.Part.createFormData("file", "avatar.png", requestFile)

            val uploadLogic = {
                dataService.secondaryApi.uploadAvatar(
                    "Bearer ${dataService.token}",
                    dataService.profile.children[dataService.currentProfile].contingentGuid,
                    part
                ).baseEnqueueOrNull {
                    loadAvatars()
                    modalDialogStateLive.postValue(false)
                }
            }

            if (dataService.avatars.isNotEmpty()) {
                dataService.secondaryApi.deleteAvatar(
                    "Bearer ${dataService.token}",
                    dataService.profile.children[dataService.currentProfile].contingentGuid,
                    dataService.avatars.first().id.toString()
                ).baseEnqueueOrNull {
                    uploadLogic()
                }
            } else {
                uploadLogic()
            }
        }
    }
}
