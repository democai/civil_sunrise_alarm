#!/bin/bash
# Test script to manually trigger the alarm broadcast and see if the service launches

echo "📡 Sending test alarm broadcast..."
adb shell am broadcast -a com.democ.civilsunrisealarm.ACTION_ALARM -n com.democ.civilsunrisealarm/.platform.alarm.AlarmReceiver

echo ""
echo "✅ Broadcast sent! Watch the device and check logs:"
echo "   adb logcat | grep -E '(AlarmReceiver|AlarmService|AlarmActivity)'"
