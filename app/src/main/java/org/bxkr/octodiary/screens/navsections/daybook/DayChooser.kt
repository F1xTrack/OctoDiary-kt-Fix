package org.bxkr.octodiary.screens.navsections.daybook

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.bxkr.octodiary.R
import org.bxkr.octodiary.modalDialogStateLive
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayChooser() {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(onDismissRequest = { modalDialogStateLive.value = false }, confirmButton = {
        TextButton(onClick = {
            val selectedDate = datePickerState.selectedDateMillis
            if (selectedDate != null) {
                daySelectedLive.value = Date(selectedDate)
                modalDialogStateLive.value = false
            }
        }) {
            Text(text = stringResource(id = R.string.select))
        }
    }) {
        DatePicker(state = datePickerState)
    }
}