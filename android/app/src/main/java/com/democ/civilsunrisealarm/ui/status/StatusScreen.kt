package com.democ.civilsunrisealarm.ui.status

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.democ.civilsunrisealarm.BuildConfig
import com.democ.civilsunrisealarm.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatusScreen(
    viewModel: StatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.status_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Divider()

        // Alarm enabled status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Alarm Enabled:",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (uiState.isEnabled) "Yes" else "No",
                style = MaterialTheme.typography.bodyLarge,
                color = if (uiState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Divider()

        // Next alarm time
        Text(
            text = stringResource(R.string.next_alarm),
            style = MaterialTheme.typography.titleMedium
        )
        val nextAlarmMillis = uiState.nextAlarmTimeMillis
        if (nextAlarmMillis != null) {
            val alarmTime = Instant.ofEpochMilli(nextAlarmMillis)
                .atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            Text(
                text = alarmTime.format(formatter),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = stringResource(R.string.no_alarm_scheduled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Divider()

        // Alarm settings summary
        Text(
            text = "Alarm Settings",
            style = MaterialTheme.typography.titleMedium
        )

        // Enabled days - table format
        Text(
            text = "Enabled Days:",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                DayOfWeek.SUNDAY to "S",
                DayOfWeek.MONDAY to "M",
                DayOfWeek.TUESDAY to "T",
                DayOfWeek.WEDNESDAY to "W",
                DayOfWeek.THURSDAY to "T",
                DayOfWeek.FRIDAY to "F",
                DayOfWeek.SATURDAY to "S"
            ).forEach { (day, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = if (day in uiState.enabledDays) "✅" else "❌",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Offset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Offset:",
                style = MaterialTheme.typography.bodyLarge
            )
            val offset = uiState.offsetMinutes
            Text(
                text = when {
                    offset > 0 -> "${offset} min after dawn"
                    offset < 0 -> "${-offset} min before dawn"
                    else -> "At civil dawn"
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }


        // Skip status
        if (uiState.skipNextAlarm) {
            Divider()

            Text(
                text = "Next alarm will be skipped",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Divider()

        // Location and solar info
        Text(
            text = "Current Details",
            style = MaterialTheme.typography.titleMedium
        )
        if (uiState.locationState.hasLocation()) {
            Text(
                text = "Lat: ${String.format("%.4f", uiState.locationState.latitude)}, " +
                        "Lon: ${String.format("%.4f", uiState.locationState.longitude)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Solar Noon
            val debugSolarNoonMillis = uiState.debugSolarNoonMillis
            if (debugSolarNoonMillis != null) {
                val solarNoonTime = Instant.ofEpochMilli(debugSolarNoonMillis)
                    .atZone(ZoneId.systemDefault())
                val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Solar Noon:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = solarNoonTime.format(timeFormatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Civil Dawn
            val debugCivilDawnMillis = uiState.debugCivilDawnMillis
            if (debugCivilDawnMillis != null) {
                val civilDawnTime = Instant.ofEpochMilli(debugCivilDawnMillis)
                    .atZone(ZoneId.systemDefault())
                val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Civil Dawn:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = civilDawnTime.format(timeFormatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val lastUpdatedMillis = uiState.locationState.lastUpdatedAtMillis
            if (lastUpdatedMillis != null) {
                val updateTime = Instant.ofEpochMilli(lastUpdatedMillis)
                    .atZone(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                Text(
                    text = "Updated: ${updateTime.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = stringResource(R.string.location_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Divider()

        // Debug information (only in debug builds)
        if (BuildConfig.DEBUG) {
            val debugSolarNoonMillis = uiState.debugSolarNoonMillis
            if (debugSolarNoonMillis != null) {
                Text(
                    text = "Debug Info",
                    style = MaterialTheme.typography.titleMedium
                )

                // Hour angle
                uiState.debugHourAngle?.let { hourAngle ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hour Angle (rad):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format("%.4f", hourAngle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Hours from noon
                uiState.debugHoursFromNoon?.let { hoursFromNoon ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hours from Noon:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = String.format("%.4f", hoursFromNoon),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Divider()
            }
        }
    }
}

