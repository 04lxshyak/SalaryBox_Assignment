package com.salarybox.attendance.location

/**
 * Structured result of an on-device location request.
 */
sealed class LocationResult {

    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val addressDescription: String? = null
    ) : LocationResult()

    data class Error(
        val errorType: LocationErrorType,
        val message: String
    ) : LocationResult()
}

enum class LocationErrorType {
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    UNAVAILABLE,
    POOR_ACCURACY
}
