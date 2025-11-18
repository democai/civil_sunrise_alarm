package com.democ.civilsunrisealarm.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.democ.civilsunrisealarm.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location provider that fetches current location using FusedLocationProvider.
 * IMPORTANT: This should only be called from the Settings screen, never from background tasks.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Checks if location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fetches current location and updates the repository.
     * This is a one-shot location request.
     * 
     * @return true if location was successfully fetched and saved, false otherwise
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchAndSaveCurrentLocation(): Boolean {
        if (!hasLocationPermission()) {
            return false
        }

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            location?.let {
                locationRepository.updateLocation(it.latitude, it.longitude)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

