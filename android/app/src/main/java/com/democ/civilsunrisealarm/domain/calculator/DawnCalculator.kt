package com.democ.civilsunrisealarm.domain.calculator

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of civil dawn calculation with debug information
 */
data class DawnCalculationResult(
    val dawnTime: LocalDateTime?,
    val solarNoonMillis: Long?,
    val hourAngle: Double?,
    val hoursFromNoon: Double?
)

/**
 * Calculates civil dawn time using astronomical formulas.
 * Civil dawn occurs when the sun is 6 degrees below the horizon.
 */
@Singleton
class DawnCalculator @Inject constructor() {

    companion object {
        private const val CIVIL_TWILIGHT_ANGLE = -6.0 // degrees below horizon
    }

    /**
     * Calculates civil dawn time for a given date and location.
     * Civil dawn is when the sun is 6 degrees below the horizon.
     *
     * @param latitude Latitude in degrees
     * @param longitude Longitude in degrees
     * @param date The date for which to calculate civil dawn
     * @param timeZone The timezone for the calculation
     * @return LocalDateTime representing civil dawn time, or null if calculation fails
     */
    fun calculateCivilDawn(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timeZone: ZoneId
    ): LocalDateTime? {
        return calculateCivilDawnWithDebug(latitude, longitude, date, timeZone).dawnTime
    }

    /**
     * Calculates civil dawn time with debug information
     */
    fun calculateCivilDawnWithDebug(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timeZone: ZoneId
    ): DawnCalculationResult {
        return try {
            val latRad = Math.toRadians(latitude)
            val angleRad = Math.toRadians(CIVIL_TWILIGHT_ANGLE)

            // Calculate day of year
            val dayOfYear = date.dayOfYear
            val year = date.year

            // Solar declination
            val declination = calculateSolarDeclination(dayOfYear)

            // Hour angle for civil dawn
            val hourAngle = calculateHourAngle(latRad, declination, angleRad)
            if (hourAngle.isNaN() || hourAngle.isInfinite()) {
                return DawnCalculationResult(null, null, null, null)
            }

            // Solar noon
            val solarNoon = calculateSolarNoon(longitude, dayOfYear, year, timeZone)

            // Hour angle to hours (convert from radians)
            val hoursFromNoon = hourAngle * 12.0 / PI

            // Civil dawn time (subtract hours from noon, convert hours to milliseconds)
            val millisFromNoon = hoursFromNoon * 60.0 * 60.0 * 1000.0
            val dawnTime = solarNoon - millisFromNoon

            // Convert to LocalDateTime
            val zonedDateTime = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(dawnTime.toLong()),
                timeZone
            )

            DawnCalculationResult(
                dawnTime = zonedDateTime.toLocalDateTime(),
                solarNoonMillis = solarNoon.toLong(),
                hourAngle = hourAngle,
                hoursFromNoon = hoursFromNoon
            )
        } catch (e: Exception) {
            DawnCalculationResult(null, null, null, null)
        }
    }

    private fun calculateSolarDeclination(dayOfYear: Int): Double {
        // Spencer 1971 formula for solar declination (accurate to ±0.0006°)
        // Returns declination in radians
        val gamma = 2.0 * PI * (dayOfYear - 1.0) / 365.0
        val declinationRad = 0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) +
                0.000907 * sin(2.0 * gamma) -
                0.00221 * cos(3.0 * gamma)
        return declinationRad
    }

    private fun calculateHourAngle(
        latitudeRad: Double,
        declinationRad: Double,
        angleRad: Double
    ): Double {
        val cosH = (sin(angleRad) - sin(latitudeRad) * sin(declinationRad)) /
                (cos(latitudeRad) * cos(declinationRad))
        
        if (cosH < -1.0 || cosH > 1.0) {
            return Double.NaN // No sunrise/sunset on this day
        }
        
        return acos(cosH)
    }

    private fun calculateSolarNoon(
        longitude: Double,
        dayOfYear: Int,
        year: Int,
        timeZone: ZoneId
    ): Double {
        // Spencer 1971 formula for Equation of Time (returns minutes)
        val gamma = 2.0 * PI * (dayOfYear - 1.0) / 365.0
        val equationOfTime = 229.18 * (
                0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
        )

        // Standard formula for solar noon (UTC):
        // Solar_Noon_UTC = 12:00 - (lon / 15) - (EoT / 60)
        val solarNoonUTC = 12.0 - (longitude / 15.0) - (equationOfTime / 60.0)

        // Create the UTC datetime for solar noon
        val localDate = LocalDate.of(year, 1, 1).plusDays(dayOfYear - 1L)
        val solarNoonUTCDateTime = localDate.atStartOfDay(ZoneId.of("UTC"))
                .plusMinutes((solarNoonUTC * 60.0).toLong())

        // Convert from UTC to the target timezone
        val solarNoonInTimeZone = solarNoonUTCDateTime.withZoneSameInstant(timeZone)

        return solarNoonInTimeZone.toInstant().toEpochMilli().toDouble()
    }
}

