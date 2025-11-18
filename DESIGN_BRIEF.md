# Design Brief Civil Sunrise Alarm

Native Android app that:
	•	Wakes the user at civil dawn, adjusted for location.
	•	Uses its own alarm logic (not modifying system Clock alarms).
	•	Respects these constraints:
	1.	Location is only updated when the Settings page is opened.
	2.	Alarms are only recalculated/scheduled:
	•	After an alarm fires,
	•	On a periodic background task (“cron”),
	•	When the Settings page is opened or modified.

⸻

1. High-Level Architecture

Layers
	1.	Presentation layer
	•	Settings screen (UI + ViewModel/presenter)
	•	Optional simple “Status / Next alarm” screen
	•	Alarm full-screen UI (when alarm fires)
	2.	Domain layer
	•	AlarmScheduler (business logic for when and how to schedule)
	•	DawnCalculator (takes location + date → civil dawn time)
	•	SettingsManager (read/write user settings)
	•	AlarmStateManager (tracks next alarm, skipped state, last location)
	3.	Platform / infrastructure
	•	Alarm backend (Android AlarmManager)
	•	Periodic worker (“cron”) via WorkManager or similar
	•	Location provider (Fused Location, or equivalent)
	•	Persistent storage (SharedPreferences or small DB)

⸻

2. Core Concepts & Data Model

2.1 User Settings

Persisted configuration object (e.g., UserSettings):
	•	isEnabled: Boolean
	•	enabledDaysOfWeek: Set<DayOfWeek>
	•	Example values: Monday–Sunday.
	•	offsetMinutesFromCivilDawn: Int
	•	Positive = minutes after civil dawn; negative = minutes before.
	•	skipNextAlarm: Boolean
	•	True = next coming alarm should be skipped once, then auto-reset to false.

Settings are modified only through the Settings screen.

2.2 Location State

Persisted location snapshot (e.g., LocationState):
	•	latitude: Double?
	•	longitude: Double?
	•	lastUpdatedAtMillis: Long? (for debug / display only)

Rules:
	•	Location may be null (e.g. first run, permissions denied).
	•	Location is updated only when Settings screen is opened, never from background cron or alarm code.

2.3 Alarm State

Persisted alarm state (e.g., AlarmState):
	•	nextAlarmTimeMillis: Long? (UTC epoch milliseconds)
	•	lastAlarmTriggeredAtMillis: Long?
	•	lastComputationDate: LocalDate? (date for which nextAlarmTime was computed)
	•	skipNextAlarmAppliedForDate: LocalDate?
	•	Tracks which date the “skip” has been applied to so the flag isn’t reused incorrectly.

⸻

3. Functional Requirements

3.1 Settings Screen

UI elements:
	•	Master toggle:
	•	“Enable civil dawn alarm” (on/off).
	•	Day-of-week selector:
	•	Seven toggles/checkboxes for Monday–Sunday.
	•	Offset control:
	•	Numeric input or slider: “Minutes offset from civil dawn” (negative allowed).
	•	Skip-next button:
	•	Button: “Skip next alarm”.
	•	Read-only info:
	•	Last known location (city/lat-lon or generic),
	•	Next scheduled alarm time (local time) or a message like “No alarm scheduled”.

Behavior:
	•	When Settings screen is opened:
	1.	Request location permission if not granted.
	2.	If permission is granted:
	•	Fetch current location (single-shot).
	•	Update LocationState with new coordinates.
	•	Trigger alarm recalculation (via AlarmScheduler).
	3.	If permission is denied:
	•	Do not update location.
	•	Alarm recalculation still runs but uses last known location or fails gracefully.
	•	When any setting is modified (enabled flag, days, offset):
	•	Persist updated UserSettings.
	•	Trigger alarm recalculation.
	•	If alarm disabled, cancel any previously scheduled alarm (in AlarmManager) and clear nextAlarmTimeMillis.
	•	Skip-next button:
	•	Set skipNextAlarm = true in UserSettings.
	•	Trigger alarm recalculation (so the next date is skipped and the following one is scheduled).
	•	UI updates to reflect that the next alarm will be skipped (e.g., a label showing which date is being skipped).

3.2 Alarm Scheduling Logic (Domain)

AlarmScheduler computes and sets the next alarm based on:

Inputs:
	•	UserSettings
	•	LocationState
	•	current time
	•	(Optionally) AlarmState for skip tracking

Steps conceptually:
	1.	If isEnabled is false:
	•	Cancel all app alarms via AlarmManager.
	•	Clear nextAlarmTimeMillis.
	•	Return.
	2.	Validate that LocationState has lat/lon:
	•	If missing:
	•	Do not schedule a new alarm.
	•	Store nextAlarmTimeMillis = null.
	•	The app should show “Location unavailable; cannot schedule alarm.”
	3.	Find the next date in the future that:
	•	Is one of the user’s enabled days,
	•	Is not “skipped” by the current skipNextAlarm flag.
	4.	For that date:
	•	Compute civil dawn time using DawnCalculator (lat, lon, date, device timezone).
	•	Apply offset (minutes).
	•	Ensure result is in the future (if not, move to next valid day).
	•	Once final date/time is found, schedule with AlarmManager and store nextAlarmTimeMillis.
	5.	Skip handling:
	•	When skipNextAlarm is true and a candidate date is encountered:
	•	Mark that date as skipNextAlarmAppliedForDate.
	•	Do not schedule for that date.
	•	Clear skipNextAlarm in settings (one-time effect).
	•	Continue searching for the next valid day and schedule that instead.

3.3 Alarm Trigger Handling

When the alarm fires (AlarmManager callback):
	•	Start full-screen Alarm Activity:
	•	Wake screen, play sound, show date/time and “Dawn alarm” label.
	•	Provide buttons: Snooze, Dismiss.
	•	Core behavior:
	1.	Record lastAlarmTriggeredAtMillis.
	2.	Regardless of whether user snoozes or dismisses:
	•	After the main alarm event is considered “done” (dismiss pressed or timer expired), AlarmScheduler is invoked to compute the next alarm, following the same rules as above.
	3.	Skip logic should already have been applied at scheduling time, so trigger handler doesn’t deal with the skip flag beyond maybe showing current state in UI.

⸻

4. Cron / Periodic Background Job

A periodic background worker (“cron”) is responsible for sanity-checking alarms and rescheduling if needed.

Key point from requirements:
Alarms can be updated in the following cases only:
	1.	After alarm trigger,
	2.	On cron,
	3.	When Settings page is opened or modified.

Cron worker behavior:
	•	Triggered once per day (or another low-frequency period, e.g. every 12 hours) via WorkManager.
	•	Does not request location updates. It uses the last stored coordinates.
	•	Tasks:
	1.	Load UserSettings, LocationState, AlarmState.
	2.	If isEnabled is false, exit (optionally cancel any alarms).
	3.	If location is null, exit (cannot compute dawn).
	4.	If nextAlarmTimeMillis is:
	•	Missing,
	•	In the past,
	•	Or inconsistent with the current rules (e.g., day-of-week changed),
then invoke AlarmScheduler to recompute and schedule.
	5.	If everything is consistent and in the future, do nothing.

This ensures alarms self-heal even if the device reboots or something went out of sync.

⸻

5. Location Update Constraints

To respect the “only update location when settings page opened” rule:
	•	The Location provider is never called from:
	•	Cron worker,
	•	Alarm trigger handler,
	•	App startup without Settings screen.
	•	Only the Settings screen (or its ViewModel) is allowed to:
	•	Request location permission.
	•	Start a one-shot location request.
	•	Save the result to LocationState.
	•	Immediately call AlarmScheduler after location changes.

Any other logic that wants to “refresh” the alarm must use the last known stored location only.

⸻

6. Screens & Components

6.1 Settings Screen

Responsibilities:
	•	Display and edit UserSettings.
	•	Trigger location update on entry (subject to permissions).
	•	Show last known location and next alarm.
	•	Provide “Skip next alarm” button and reflect its status.

Interactions:
	•	On open:
	•	Read UserSettings and populate UI.
	•	Start location fetch (optional if permission already granted).
	•	After location update, call AlarmScheduler.
	•	On change:
	•	Update settings.
	•	Invoke AlarmScheduler.
	•	On “Skip next alarm”:
	•	Set skipNextAlarm = true.
	•	Invoke AlarmScheduler.

6.2 Status/Overview Screen (optional but recommended)
	•	Shows:
	•	Whether the alarm is enabled.
	•	Next scheduled alarm time, expressed in local time and date.
	•	Whether “next alarm is being skipped”.
	•	Last known location info.
	•	Read-only; manipulations are done on Settings screen.

6.3 Alarm Activity (Full-screen)
	•	Triggered by AlarmManager.
	•	Visual layout with:
	•	“Good morning” or similar,
	•	Time and date,
	•	“Civil dawn + offset” info (optional).
	•	Actions:
	•	Dismiss: stops sound and triggers AlarmScheduler to schedule the next day.
	•	Snooze: local short-term snooze; when snooze fires, it should not schedule the next full-day alarm again — the next-day scheduling still follows standard behavior after the main alarm cycle (you can clarify snooze semantics in implementation).

⸻

7. Cursor Agent Work Breakdown (for you to feed in as tasks)

You can split work as separate agent tasks like:
	1.	Spec → Data Models
	•	Define UserSettings, LocationState, AlarmState.
	•	Define persistence interfaces, no platform details.
	2.	Spec → Domain Logic
	•	Formalize AlarmScheduler API:
	•	Inputs: settings, location, now, optional state.
	•	Outputs: next alarm timestamp or “none”.
	•	Encapsulate skip-next logic and next-date search.
	•	Formalize DawnCalculator API.
	3.	Spec → Android Integration
	•	Map domain scheduleAlarmAt(timeMillis) to AlarmManager with appropriate type.
	•	Define WorkManager periodic job that calls AlarmScheduler with existing state.
	•	Define BroadcastReceiver and Alarm Activity contracts.
	4.	Spec → Settings Screen UX
	•	Detail UI components and state transitions.
	•	Define when and how location permission is requested.
	•	Ensure location fetch is only invoked in Settings.

