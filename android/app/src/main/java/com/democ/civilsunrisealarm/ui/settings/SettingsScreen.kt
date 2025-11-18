package com.democ.civilsunrisealarm.ui.settings

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.democ.civilsunrisealarm.R
import com.democ.civilsunrisealarm.ui.alarm.AlarmActivity
import com.democ.civilsunrisealarm.ui.components.DayOfWeekSelector
import com.democ.civilsunrisealarm.ui.components.NextAlarmDisplay
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val alarmState by viewModel.alarmState.collectAsState()

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, fetch location
            viewModel.fetchLocationIfPermitted()
        }
    }

    // Request permission when screen opens if not granted
    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permission already granted, fetch location
            viewModel.fetchLocationIfPermitted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // Master toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.enable_alarm),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = viewModel::updateEnabled
            )
        }

        Divider()

        // Day of week selector
        Text(
            text = stringResource(R.string.days_of_week),
            style = MaterialTheme.typography.titleMedium
        )
        DayOfWeekSelector(
            enabledDays = uiState.enabledDays,
            onDayToggled = viewModel::updateDayEnabled
        )

        Divider()

        // Offset control
        Text(
            text = stringResource(R.string.offset_minutes),
            style = MaterialTheme.typography.titleMedium
        )
        var offsetText by remember { mutableStateOf(uiState.offsetMinutes.toString()) }
        var isSavingOffset by remember { mutableStateOf(false) }
        var lastSavedOffset by remember { mutableStateOf(uiState.offsetMinutes) }

        // Update text when state changes externally
        LaunchedEffect(uiState.offsetMinutes) {
            if (uiState.offsetMinutes != lastSavedOffset) {
                offsetText = uiState.offsetMinutes.toString()
                lastSavedOffset = uiState.offsetMinutes
            }
        }

        // Debounced save effect
        LaunchedEffect(offsetText) {
            val newValue = offsetText.toIntOrNull()
            if (newValue != null && newValue != lastSavedOffset) {
                isSavingOffset = true
                delay(500) // Debounce for 500ms
                if (offsetText.toIntOrNull() == newValue) { // Check value hasn't changed during delay
                    viewModel.updateOffsetMinutes(newValue)
                    lastSavedOffset = newValue
                }
                isSavingOffset = false
            }
        }

        TextField(
            value = offsetText,
            onValueChange = { newValue ->
                // Only allow valid integer input (including negative)
                if (newValue.isEmpty() || newValue == "-" || newValue.toIntOrNull() != null) {
                offsetText = newValue
                }
            },
            label = { Text("Minutes from civil dawn") },
            supportingText = {
                val offset = offsetText.toIntOrNull() ?: uiState.offsetMinutes
                when {
                    offset > 0 -> Text("${offset} minutes after civil dawn")
                    offset < 0 -> Text("${-offset} minutes before civil dawn")
                    else -> Text("At civil dawn (no offset)")
                }
            },
            trailingIcon = {
                when {
                    isSavingOffset -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    offsetText.toIntOrNull() == lastSavedOffset && offsetText.isNotEmpty() -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = Color(0xFF4CAF50) // Green color
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
        Text(
            text = "Use negative values (e.g., -15) to wake before dawn, positive values (e.g., +30) to wake after dawn",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // Skip next alarm button
        Button(
            onClick = {
                if (uiState.skipNextAlarm) {
                    viewModel.unskipNextAlarm()
                } else {
                    viewModel.skipNextAlarm()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.skipNextAlarm) {
                    stringResource(R.string.unskip_next_alarm)
                } else {
                    stringResource(R.string.skip_next_alarm)
                }
            )
        }

        // Show skip feedback if skip is active
        val skipDate = uiState.skipNextAlarmAppliedForDate
        if (uiState.skipNextAlarm && skipDate != null) {
            Text(
                text = stringResource(
                    R.string.skip_alarm_feedback,
                    skipDate.format(
                        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
                    )
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else if (uiState.skipNextAlarm) {
            Text(
                text = stringResource(R.string.skip_alarm_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Test alarm button
        val context = LocalContext.current
        Button(
            onClick = {
                val testAlarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("triggered", false)
                    putExtra("isSnooze", false)
                }
                context.startActivity(testAlarmIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.test_alarm))
        }

        if (uiState.isLoadingLocation) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (!uiState.locationState.hasLocation()) {
            Text(
                text = stringResource(R.string.location_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!uiState.hasLocationPermission) {
            Text(
                text = stringResource(R.string.location_permission_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }


        // Exact alarm permission
        if (!uiState.canScheduleExactAlarms) {
            Divider()
            Text(
                text = stringResource(R.string.exact_alarm_permission_required),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.exact_alarm_permission_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val intent = viewModel.requestExactAlarmPermission()
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_exact_alarm_permission))
            }
        }

        Divider()

        // Next alarm display
        NextAlarmDisplay(alarmState = alarmState)
    }
}

