package com.democ.civilsunrisealarm.ui.alarm

import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.democ.civilsunrisealarm.R
import com.democ.civilsunrisealarm.platform.alarm.AlarmManagerWrapper
import com.democ.civilsunrisealarm.platform.alarm.AlarmTriggerHandler
import com.democ.civilsunrisealarm.ui.theme.CivilSunriseAlarmTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var alarmTriggerHandler: AlarmTriggerHandler

    @Inject
    lateinit var alarmManagerWrapper: AlarmManagerWrapper

    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtone: android.media.Ringtone? = null
    private var isSnooze: Boolean = false
    private var wakeLockReleased: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this was triggered by alarm or snooze
        val wasTriggered = intent.getBooleanExtra("triggered", false)
        isSnooze = intent.getBooleanExtra("isSnooze", false)
        
        // Only trigger next alarm scheduling for main alarm, not snooze
        if (wasTriggered && !isSnooze) {
            alarmTriggerHandler.onAlarmTriggered()
        }

        // API 27+ lock screen flags (required for Android 14/15 full-screen intent pattern)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Wake up screen and keep it on (legacy support for older APIs)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Acquire wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CivilSunriseAlarm::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes

        // Play alarm sound
        playAlarmSound()

        setContent {
            CivilSunriseAlarmTheme {
                AlarmScreen(
                    onDismiss = {
                        dismissAlarm()
                    },
                    onSnooze = {
                        snoozeAlarm()
                    }
                )
            }
        }
    }

    private fun playAlarmSound() {
        try {
            // Try to get default alarm sound
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            // If no default alarm, try notification sound
            if (alarmUri == null) {
                Log.w("AlarmActivity", "No default alarm sound, trying notification sound")
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            
            // If still null, try ringtone
            if (alarmUri == null) {
                Log.w("AlarmActivity", "No notification sound, trying ringtone")
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            
            if (alarmUri == null) {
                Log.e("AlarmActivity", "No ringtone available on device")
                return
            }
            
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            
            if (ringtone == null) {
                Log.e("AlarmActivity", "Failed to create ringtone from URI: $alarmUri")
                return
            }
            
            // Set audio attributes for alarm usage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone?.audioAttributes = audioAttributes
            } else {
                @Suppress("DEPRECATION")
                ringtone?.streamType = AudioManager.STREAM_ALARM
            }
            
            // Enable looping (API 28+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            
            // Play the ringtone
            ringtone?.play()
            Log.d("AlarmActivity", "Alarm sound started playing")
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error playing alarm sound", e)
        }
    }

    private fun dismissAlarm() {
        ringtone?.stop()
        cancelAlarmNotification()
        releaseWakeLock()
        // Alarm trigger was already handled in onCreate if this was from AlarmReceiver
        finish()
    }

    private fun snoozeAlarm() {
        ringtone?.stop()
        cancelAlarmNotification()
        // Snooze for 5 minutes
        val snoozeTimeMillis = System.currentTimeMillis() + (5 * 60 * 1000L)
        // Schedule a temporary alarm for snooze
        // This uses a separate request code and action so it doesn't interfere with the main alarm
        try {
            alarmManagerWrapper.scheduleSnoozeAlarm(snoozeTimeMillis)
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Failed to schedule snooze alarm", e)
            // Continue to dismiss even if snooze scheduling fails
        }
        releaseWakeLock()
        finish()
    }
    
    private fun cancelAlarmNotification() {
        try {
            val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            // Cancel the notification posted by AlarmReceiver (NOTIFICATION_ID = 1001)
            notificationManager?.cancel(1001)
            Log.d("AlarmActivity", "✅ Alarm notification cancelled")
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error cancelling alarm notification", e)
        }
    }

    private fun releaseWakeLock() {
        if (!wakeLockReleased && wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
                wakeLockReleased = true
            } catch (e: Exception) {
                Log.e("AlarmActivity", "Error releasing WakeLock", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        cancelAlarmNotification()
        releaseWakeLock()
    }
}

@Composable
fun AlarmScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val currentTime = remember {
        Instant.now().atZone(ZoneId.systemDefault())
    }
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = stringResource(R.string.good_morning),
                style = MaterialTheme.typography.displayLarge,
                fontSize = 48.sp
            )

            Text(
                text = stringResource(R.string.civil_dawn_alarm),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = currentTime.format(timeFormatter),
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = currentTime.format(dateFormatter),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.alarm_snooze))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.alarm_dismiss))
                }
            }
        }
    }
}

