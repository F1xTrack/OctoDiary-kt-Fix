package org.bxkr.octodiary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.bxkr.octodiary.CachePrefs
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.models.classmembers.Assignment
import org.bxkr.octodiary.models.classmembers.ClassMember
import org.bxkr.octodiary.models.classmembers.OctoClassMembers
import org.bxkr.octodiary.models.profile.ProfileResponse
import org.bxkr.octodiary.models.classranking.RankingMember
import org.bxkr.octodiary.save

class ClassInfoViewModelFactory(
    private val application: Application,
    private val dataService: DataService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassInfoViewModel(application, dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ClassInfoViewModel(
    application: Application,
    private val dataService: DataService
) : AndroidViewModel(application) {

    val classMembers: StateFlow<List<ClassMember>> = dataService.classMembers
    val profile: StateFlow<ProfileResponse?> = dataService.profile
    val currentProfileIndex: StateFlow<Int> = dataService.currentProfile
    val ranking: StateFlow<List<RankingMember>> = dataService.ranking

    fun assignPersonId(studentId: Long, personId: String, cachePrefs: CachePrefs, onUpdated: () -> Unit) {
        viewModelScope.launch {
            val classMember =
                dataService.classMembers.value.first { it.studentId == studentId }.copy(personId = personId)
            val newClassMembers =
                dataService.classMembers.value.filter { it.studentId != studentId } + listOf(classMember)
            val assignments = newClassMembers
                .filter { it.personId != null && it.studentId != null }
                .map { Assignment(it.studentId!!, it.personId!!) }

            // Update DataService's internal StateFlow
            (dataService.classMembers as? MutableStateFlow<List<ClassMember>>)?.value = newClassMembers

            cachePrefs.save("classMembers" to newClassMembers.let { Gson().toJson(it) })
            dataService.pushUserSettings("od_class_members_assignments", OctoClassMembers(assignments), onUpdated = onUpdated)
        }
    }
}
