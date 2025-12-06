package org.bxkr.octodiary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.models.classmembers.ClassMember
import org.bxkr.octodiary.models.marklistdate.MarkListDate
import org.bxkr.octodiary.models.marklistsubject.MarkListSubjectItem
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.models.subjectranking.SubjectRanking

class MarksViewModelFactory(
    private val application: Application,
    private val dataService: DataService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarksViewModel(application, dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MarksViewModel(
    application: Application,
    private val dataService: DataService
) : AndroidViewModel(application) {

    val marksSubject: StateFlow<List<MarkListSubjectItem>> = dataService.marksSubjectFlow
    val marksDate: StateFlow<MarkListDate?> = dataService.marksDateFlow
    val subjectRanking: StateFlow<List<SubjectRanking>> = dataService.subjectRanking
    val classMembers: StateFlow<List<ClassMember>> = dataService.classMembers
    val profile: StateFlow<ProfileResponse?> = dataService.profile
    val currentProfileIndex: StateFlow<Int> = dataService.currentProfile
}