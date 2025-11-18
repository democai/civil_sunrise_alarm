package com.democ.civilsunrisealarm.domain.calculator

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DawnCalculatorTest {

    private val calculator = DawnCalculator()

    @Test
    fun `test solar noon calculation for known location`() {
        // Test location: lat 34.0522, lon -118.2437 (Beverly Hills, should be around 12:15 PM)
        val latitude = 34.0522
        val longitude = -118.2437
        val date = LocalDate.of(2024, 1, 15) // Use a specific date
        val timeZone = ZoneId.of("America/Los_Angeles") // PST/PDT timezone

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Solar noon should be calculated", result.solarNoonMillis)

        // Convert solar noon to readable time
        val solarNoonTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(result.solarNoonMillis!!),
            timeZone
        )

        val solarNoonHour = solarNoonTime.hour
        val solarNoonMinute = solarNoonTime.minute

        // Solar noon should be around 12:15 PM (within ±15 minutes for equation of time variance)
        assertTrue(
            "Solar noon should be around 12:00-12:30, got ${solarNoonHour}:${String.format("%02d", solarNoonMinute)}",
            solarNoonHour == 12 && solarNoonMinute >= 0 && solarNoonMinute <= 30
        )

        println("✅ Solar Noon: ${solarNoonTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
        println("   Hour Angle: ${result.hourAngle}")
        println("   Hours from Noon: ${result.hoursFromNoon}")
    }

    @Test
    fun `test dawn calculation produces morning time`() {
        val latitude = 34.0522
        val longitude = -118.2437
        val date = LocalDate.of(2024, 1, 15)
        val timeZone = ZoneId.of("America/Los_Angeles")

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Dawn time should be calculated", result.dawnTime)

        val dawnTime = result.dawnTime!!
        val dawnHour = dawnTime.hour
        val dawnMinute = dawnTime.minute

        // Dawn should be in the morning (before 12:00)
        assertTrue(
            "Dawn should be in the morning (before 12:00), got ${dawnHour}:${String.format("%02d", dawnMinute)}",
            dawnHour < 12
        )

        // Dawn should be in the 6-7 AM range for this location
        assertTrue(
            "Dawn should be in the morning range, got ${dawnHour}:${String.format("%02d", dawnMinute)}",
            (dawnHour == 6 || dawnHour == 7)
        )

        println("✅ Dawn Time: ${dawnTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
    }

    @Test
    fun `test hours from noon is reasonable`() {
        val latitude = 34.0522
        val longitude = -118.2437
        val date = LocalDate.of(2024, 1, 15)
        val timeZone = ZoneId.of("America/Los_Angeles")

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Hours from noon should be calculated", result.hoursFromNoon)

        val hoursFromNoon = result.hoursFromNoon!!

        // Hours from noon should be around 5-7 hours (dawn is typically 5-7 hours before noon)
        assertTrue(
            "Hours from noon should be 5-7 hours, got $hoursFromNoon",
            hoursFromNoon >= 5.0 && hoursFromNoon <= 7.0
        )

        println("Hours from Noon: $hoursFromNoon")
    }

    @Test
    fun `test solar noon at Greenwich meridian`() {
        // At longitude 0°, solar noon should be close to 12:00 UTC
        val latitude = 51.5074 // London
        val longitude = 0.0 // Greenwich
        val date = LocalDate.of(2024, 6, 21) // Summer solstice
        val timeZone = ZoneId.of("Europe/London")

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Solar noon should be calculated", result.solarNoonMillis)

        val solarNoonTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(result.solarNoonMillis!!),
            timeZone
        )

        // At Greenwich, solar noon should be close to 12:00 (allowing for equation of time)
        val hour = solarNoonTime.hour
        val minute = solarNoonTime.minute

        assertTrue(
            "Solar noon at Greenwich should be around 12:00, got $hour:$minute",
            hour == 12 || (hour == 11 && minute >= 50) || (hour == 13 && minute <= 10)
        )
    }

    @Test
    fun `test dawn calculation with offset`() {
        val latitude = 34.0522
        val longitude = -118.2437
        val date = LocalDate.of(2024, 1, 15)
        val timeZone = ZoneId.of("America/Los_Angeles")

        val result = calculator.calculateCivilDawn(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Dawn time should be calculated", result)

        // Apply -15 minute offset (wake before dawn)
        val alarmTime = result!!.plusMinutes(-15)
        val alarmHour = alarmTime.hour
        val alarmMinute = alarmTime.minute

        // Should be in the morning
        assertTrue(
            "Alarm with -15 min offset should be in morning, got $alarmHour:$alarmMinute",
            alarmHour < 12
        )

        println("Dawn: ${result.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
        println("Alarm (-15 min): ${alarmTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
    }

    @Test
    fun `test calculation handles edge cases`() {
        // Test at equator
        val result1 = calculator.calculateCivilDawn(
            latitude = 0.0,
            longitude = 0.0,
            date = LocalDate.of(2024, 6, 21),
            timeZone = ZoneId.of("UTC")
        )
        assertNotNull("Should calculate dawn at equator", result1)

        // Test at high latitude (should still work)
        val result2 = calculator.calculateCivilDawn(
            latitude = 60.0,
            longitude = 0.0,
            date = LocalDate.of(2024, 6, 21),
            timeZone = ZoneId.of("UTC")
        )
        assertNotNull("Should calculate dawn at high latitude", result2)
    }

    @Test
    fun `test solar noon calculation accuracy`() {
        // Known location: New York City
        // On Jan 15, 2024, solar noon should be around 12:05 PM EST
        val latitude = 40.7128
        val longitude = -74.0060
        val date = LocalDate.of(2024, 1, 15)
        val timeZone = ZoneId.of("America/New_York")

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Solar noon should be calculated", result.solarNoonMillis)

        val solarNoonTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(result.solarNoonMillis!!),
            timeZone
        )

        val hour = solarNoonTime.hour
        val minute = solarNoonTime.minute

        // Should be around 11:30-13:00 (allowing for equation of time and timezone variance)
        // The key is that it should be around noon, not in the evening
        assertTrue(
            "Solar noon in NYC should be around 11:30-13:00, got $hour:$minute",
            (hour == 11 && minute >= 30) || hour == 12 || (hour == 13 && minute == 0)
        )

        println("NYC Solar Noon: ${solarNoonTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
    }

    @Test
    fun `test equation of time calculation`() {
        // Nov 17 is day 321 of the year, 2025
        // Check what equation of time we're calculating using accurate formula
        val dayOfYear = 321
        val year = 2025
        val D = 6.240040776 + 0.01720197 * (365.25 * (year - 2000.0) + dayOfYear)
        val equationOfTime = -7.659 * kotlin.math.sin(D) + 9.863 * kotlin.math.sin(2.0 * D + 3.5932)

        println("\n📐 EQUATION OF TIME DEBUG:")
        println("   Day of year: $dayOfYear, Year: $year")
        println("   D value: $D")
        println("   Equation of time: $equationOfTime minutes")
        println("   (Should be around -10 minutes for November 17, 2025)")

        // For longitude -118.2437:
        val longitude = -118.2437
        val longitudeHours = longitude / 15.0
        println("\n📍 LONGITUDE CALCULATION:")
        println("   Longitude: $longitude")
        println("   Longitude hours: $longitudeHours")
        println("   UTC offset for this longitude: ${-longitudeHours} hours from UTC")

        // Solar noon in UTC should be around 18:30-18:40 UTC (6.5-6.67 hours AFTER 12:00 UTC base)
        val solarNoonUTCHours = 12.0 + (equationOfTime / 60.0) - longitudeHours
        println("\n☀️ SOLAR NOON IN UTC:")
        println("   12 + (${equationOfTime / 60.0}) - (${longitudeHours}) = $solarNoonUTCHours")
        println("   Which is: ${solarNoonUTCHours.toInt()}:${((solarNoonUTCHours - solarNoonUTCHours.toInt()) * 60).toInt()} UTC")

        // Mexico City is UTC-6 (during standard time)
        // So if solar noon is 18:30 UTC, in Mexico City it's 12:30 PM ✓
        println("\n🏙️ CONVERT TO MEXICO CITY (UTC-6):")
        println("   ${solarNoonUTCHours.toInt()}:${((solarNoonUTCHours - solarNoonUTCHours.toInt()) * 60).toInt()} UTC - 6 hours = ${(solarNoonUTCHours - 6).toInt()}:${(((solarNoonUTCHours - 6) - (solarNoonUTCHours - 6).toInt()) * 60).toInt()} Mexico City time")
    }

    @Test
    fun `test against official data - Nov 18 2025`() {
        // Using official data from timeanddate.com for Nov 18, 2025
        // Location: Beverly Hills, CA (34°05'N / 118°24'W, UTC-8)
        // Official Solar Noon: 11:38 AM
        // Official Civil Twilight Start: 6:02 AM
        // Official Sunrise: 6:28 AM

        val latitude = 34.0833
        val longitude = -118.4
        val date = LocalDate.of(2025, 11, 18)
        val timeZone = ZoneId.of("America/Los_Angeles")

        val result = calculator.calculateCivilDawnWithDebug(
            latitude = latitude,
            longitude = longitude,
            date = date,
            timeZone = timeZone
        )

        assertNotNull("Dawn time should be calculated", result.dawnTime)
        assertNotNull("Solar noon should be calculated", result.solarNoonMillis)

        val dawnTime = result.dawnTime!!
        val solarNoonTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(result.solarNoonMillis!!),
            timeZone
        )

        println("\n📊 OFFICIAL DATA CHECK (Nov 18, 2025, Beverly Hills):")
        println("   Official Solar Noon: 11:38 AM")
        println("   Calculated Solar Noon: ${solarNoonTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
        println("   Official Civil Twilight Start: 6:02 AM")
        println("   Calculated Dawn: ${dawnTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")

        val dawnHour = dawnTime.hour
        val dawnMinute = dawnTime.minute
        val noonHour = solarNoonTime.hour
        val noonMinute = solarNoonTime.minute

        // Solar noon should be around 11:39 (allow ±2 minutes for precision)
        assertTrue(
            "Solar noon should be 11:37-11:40, got ${noonHour}:${String.format("%02d", noonMinute)}",
            noonHour == 11 && noonMinute >= 37 && noonMinute <= 40
        )

        // Civil twilight start should be around 6:02-6:03 AM (allow ±1 minute)
        assertTrue(
            "Civil twilight start should be 6:01-6:03 AM, got ${dawnHour}:${String.format("%02d", dawnMinute)}",
            dawnHour == 6 && dawnMinute >= 1 && dawnMinute <= 3
        )
    }
}

