package com.democ.civilsunrisealarm.domain.model

data class LocationState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastUpdatedAtMillis: Long? = null
) {
    fun hasLocation(): Boolean = latitude != null && longitude != null
}

