package org.bxkr.octodiary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow // Импорт TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color // Добавляем импорт Color

@Composable
fun SwitchPreference(
    title: String,
    description: String? = null,
    listenState: State<Boolean>,
    onToggled: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                onToggled(!listenState.value)
            }
    ) {
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(5f)) { // Увеличиваем вес, чтобы дать БОЛЬШЕ места тексту
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        description,
                        Modifier.alpha(.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3, // Разрешаем 3 строки для описания
                        overflow = TextOverflow.Ellipsis // Добавляем троеточие
                    )
                }
            }
            Switch(
                checked = listenState.value,
                onCheckedChange = onToggled,
                modifier = Modifier.weight(1f, false),
                colors = SwitchDefaults.colors( // Явно задаем цвета для контрастности
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
            )
        }
    }
}
