package org.bxkr.octodiary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.models.homeworks2.Homework

class HomeworksViewModelFactory(
    private val application: Application,
    private val dataService: DataService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeworksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeworksViewModel(application, dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HomeworksViewModel(
    application: Application,
    private val dataService: DataService
) : AndroidViewModel(application) {

    val homeworks: StateFlow<List<Homework>> = dataService.homeworksFlow
}
