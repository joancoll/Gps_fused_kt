package cat.dam.andy.gps_fused_kt

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient

class LocationManager(
    // Aquesta classe utilitza FusedLocationProviderClient, que és una API de Google Play Services.
    // Per tant, és necessari que l'activitat que utilitzi aquesta classe implementi l'interfície
    // mitjançant un callback del tipus LocationCallback() i capturi onLocationResult.

    private var activityContext: Context,
    private var timeInterval: Long,
    private var minimalDistance: Float,
    private var priority: Int,
    private var granularity: Int,
    private var callback: LocationCallback
) {

    private var request: LocationRequest
    private var locationClient: FusedLocationProviderClient

    init {
        // Inicialitza el client de localització i la petició al crear la classe.
        locationClient = LocationServices.getFusedLocationProviderClient(activityContext)
        request = createRequest()
    }

    // Aquesta funció crea una petició de localització amb els paràmetres que s'han passat al
    // constructor.
    private fun createRequest(): LocationRequest =
        LocationRequest.Builder(priority, timeInterval).apply {
            setMinUpdateDistanceMeters(minimalDistance)
            setGranularity(granularity)
            setWaitForAccurateLocation(true)
        }.build()

    fun changeRequest(timeInterval: Long, minimalDistance: Float) {
        // Aquesta funció permet canviar els paràmetres de la petició de localització.
        this.timeInterval = timeInterval
        this.minimalDistance = minimalDistance
        createRequest()
        stopLocationTracking()
        startLocationTracking()
    }

    fun startLocationTracking() {
        // Aquesta funció inicia el seguiment de la localització.
        // Primer comprova que els serveis de localització estiguin habilitats i, si no ho estan,
        // intenta activar-los.
        val settingsRequestBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
        val client: SettingsClient = LocationServices.getSettingsClient(activityContext)
        val locationSettingsResponseTask =
            client.checkLocationSettings(settingsRequestBuilder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Si els serveis de localització no estan habilitats, intenta activar-los.
                    exception.startResolutionForResult(
                        (activityContext as MainActivity),
                        0
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(
                        "LocationManager",
                        "Error al iniciar la resolució de la configuració de localització",
                        sendEx
                    )
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                // Inicia el seguiment de la ubicació si la configuració és vàlida
                // i els serveis de localització estan habilitats.
                // Aquesta funció requereix els permisos ACCESS_FINE_LOCATION i ACCESS_COARSE_LOCATION.
                if (ActivityCompat.checkSelfPermission(
                        activityContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        activityContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Si no es tenen els permisos, no es pot iniciar el seguiment de la localització.
                    return@addOnCompleteListener
                }
                // Inicia el seguiment de la localització.
                locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
        }
    }

    fun stopLocationTracking() {
        // Aquesta funció atura el seguiment de la localització.
        locationClient.flushLocations()
        locationClient.removeLocationUpdates(callback)
    }
}
