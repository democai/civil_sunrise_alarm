#!/bin/bash

# Script to monitor alarm-related logs in real-time
# This helps debug alarm scheduling and triggering issues

echo "🔍 Monitoring alarm-related logs..."
echo "📱 Make sure your device is connected via ADB"
echo "⏰ Set an alarm and watch for logs when it should trigger"
echo ""
echo "Looking for logs from:"
echo "  - AlarmReceiver (receives alarm broadcast)"
echo "  - AlarmManagerWrapper (schedules alarms)"
echo "  - AlarmActivity (displays alarm)"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo "----------------------------------------"
echo ""

adb logcat -c  # Clear existing logs
adb logcat | grep -E "(AlarmReceiver|AlarmManagerWrapper|AlarmActivity|AlarmCheckWorker|AlarmScheduler)" --color=always

