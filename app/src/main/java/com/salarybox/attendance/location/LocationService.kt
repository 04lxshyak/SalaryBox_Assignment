package com.salarybox.attendance.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val LOCATION_TIMEOUT_MS = 10_000L // 10 second strict timeout
    }

    /**
     * Checks if location permission is granted (either FINE or COARSE).
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Checks if GPS or Network location providers are enabled on the device.
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    /**
     * Obtains the current location with a 10-second timeout.
     * Fails closed if permission is denied, location disabled, or timed out.
     */
    suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.IO) {
        // 1. Permission Check
        if (!hasLocationPermission()) {
            return@withContext LocationResult.Error(
                LocationErrorType.PERMISSION_DENIED,
                "Location permission is required to mark attendance."
            )
        }

        // 2. Provider Enabled Check
        if (!isLocationEnabled()) {
            return@withContext LocationResult.Error(
                LocationErrorType.LOCATION_DISABLED,
                "Location services are disabled. Please turn on GPS."
            )
        }

        // 3. Request Current Location with Timeout
        val cancellationTokenSource = CancellationTokenSource()
        val location: Location? = try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                try {
                    val task = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    )
                    Tasks.await(task)
                } catch (e: Exception) {
                    null
                }
            }
        } finally {
            cancellationTokenSource.cancel()
        }

        // Attendance must be tied to a fresh capture event. A last-known location may
        // belong to an earlier place or time, so the workflow fails closed instead.
        if (location == null) {
            return@withContext LocationResult.Error(
                LocationErrorType.TIMEOUT,
                "Location could not be obtained. Attendance was not recorded."
            )
        }

        // 5. Reverse Geocode address if possible (non-blocking best effort)
        val addressDescription = getAddressDescription(location.latitude, location.longitude)

        return@withContext LocationResult.Success(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            addressDescription = addressDescription
        )
    }

    private fun getAddressDescription(latitude: Double, longitude: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull() ?: return null

            val locality = address.locality ?: address.subAdminArea ?: ""
            val state = address.adminArea ?: ""
            val country = address.countryName ?: ""

            listOf(locality, state, country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
