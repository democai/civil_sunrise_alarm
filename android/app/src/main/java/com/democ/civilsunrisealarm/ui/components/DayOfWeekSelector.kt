package com.democ.civilsunrisealarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.democ.civilsunrisealarm.R
import java.time.DayOfWeek

@Composable
fun DayOfWeekSelector(
    enabledDays: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = getDayName(day),
                    style = MaterialTheme.typography.bodyLarge
                )
                Checkbox(
                    checked = day in enabledDays,
                    onCheckedChange = { checked -> onDayToggled(day, checked) }
                )
            }
        }
    }
}

@Composable
private fun getDayName(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> stringResource(R.string.monday)
        DayOfWeek.TUESDAY -> stringResource(R.string.tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.wednesday)
        DayOfWeek.THURSDAY -> stringResource(R.string.thursday)
        DayOfWeek.FRIDAY -> stringResource(R.string.friday)
        DayOfWeek.SATURDAY -> stringResource(R.string.saturday)
        DayOfWeek.SUNDAY -> stringResource(R.string.sunday)
    }
}

