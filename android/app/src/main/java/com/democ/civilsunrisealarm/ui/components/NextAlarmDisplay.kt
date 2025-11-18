package com.democ.civilsunrisealarm.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.democ.civilsunrisealarm.R
import com.democ.civilsunrisealarm.domain.model.AlarmState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NextAlarmDisplay(alarmState: AlarmState) {
    Text(
        text = stringResource(R.string.next_alarm),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
    )
    
    if (alarmState.nextAlarmTimeMillis != null) {
        val alarmTime = Instant.ofEpochMilli(alarmState.nextAlarmTimeMillis)
            .atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        Text(
            text = alarmTime.format(formatter),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    } else {
        Text(
            text = stringResource(R.string.no_alarm_scheduled),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}

