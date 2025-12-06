package org.bxkr.octodiary.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.bxkr.octodiary.DataService
import org.bxkr.octodiary.models.events.Event

class ScheduleViewModelFactory(
    private val application: Application,
    private val dataService: DataService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(application, dataService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ScheduleViewModel(
    application: Application,
    private val dataService: DataService
) : AndroidViewModel(application) {

    // Proxy eventCalendar from DataService
    val eventCalendar: StateFlow<List<Event>> = dataService.eventCalendar

    // Logic for weeks loading
    fun updateEventCalendar(weeksBefore: Int, weeksAfter: Int, onFinish: () -> Unit) {
        viewModelScope.launch {
            dataService.updateEventCalendar(weeksBefore, weeksAfter, onFinish)
        }
    }
}
