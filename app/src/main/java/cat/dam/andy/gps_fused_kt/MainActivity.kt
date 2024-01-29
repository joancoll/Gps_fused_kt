package cat.dam.andy.gps_fused_kt

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartLocation: Button
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private val context: Context = this
    private var permissionManager = PermissionManager(context)
    private var requestingLocationUpdates = false
    private lateinit var locationManager: LocationManager
    private var locationEnabled = false // Variable de control

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initPermissions()
        initListeners()
        initLocationManager()
        updateButtonScreen()
    }

    private fun initViews() {
        btnStartLocation = findViewById(R.id.btn_startLocation)
        tvLatitude = findViewById(R.id.tv_latitude)
        tvLongitude = findViewById(R.id.tv_longitude)
    }

    private fun initPermissions() {
        permissionManager.addPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            getString(R.string.locationPermissionInfo),
            getString(R.string.locationPermissionNeeded),
            getString(R.string.locationPermissionDenied),
            getString(R.string.locationPermissionThanks),
            getString(R.string.locationPermissionSettings)
        )
    }

    private fun initListeners() {
        btnStartLocation.setOnClickListener {
            if (!permissionManager.hasAllNeededPermissions()) {
                permissionManager.askForPermissions(permissionManager.getRejectedPermissions())
            } else {
                toggleLocationUpdates()
            }
        }
    }

    private fun initLocationManager() {
        // Paràmetres de la localització
        val locUpdateTimeInterval: Long = 3000 // milliseconds
        val locUpdateMinimalDistance = 2.0f // meters
        val locPriority: Int = Priority.PRIORITY_HIGH_ACCURACY // PRIORITY_BALANCED_POWER_ACCURACY
        val locGranularity: Int = Granularity.GRANULARITY_PERMISSION_LEVEL // GRANULARITY_NONE
        // Callback de la localització que s'utilitza per rebre les actualitzacions de la localització
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // quan rep una localització actualitza el text i el botó si ha tingut exit
                locationResult.lastLocation?.let { location ->
                    updateLocationScreen(location.latitude, location.longitude)
                    if (requestingLocationUpdates) {
                        locationEnabled = true
                        updateButtonScreen()
                        requestingLocationUpdates = false
                    }
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    // quan deixa de rebre localitzacions desactiva el botó i el tracking
                    locationEnabled = false
                    updateButtonScreen()
                    locationManager.stopLocationTracking()
                }
            }
        }
        // Inicialitza el LocationManager amb els paràmetres i el callback
        locationManager = LocationManager(
            context,
            locUpdateTimeInterval,
            locUpdateMinimalDistance,
            locPriority,
            locGranularity,
            locationCallback
        )
    }

    private fun toggleLocationUpdates() {
        // Activa o desactiva el tracking de la localització segons clica l'usuari i estat actual
        if (locationEnabled) {
            locationEnabled = false
            locationManager.stopLocationTracking()
            updateButtonScreen()
        } else {
            // no canviem l'estat fins que rebem la primera localització
            requestingLocationUpdates = true
            locationManager.startLocationTracking()
        }
    }

    private fun updateButtonScreen() {
        // Actualitza el botó i el text de pantalla segons l'estat de la localització
        if (!locationEnabled) {
            btnStartLocation.text = getString(R.string.start_location_updates)
            btnStartLocation.setBackgroundColor(getColor(R.color.btn_on))
            tvLatitude.text = "-----"
            tvLongitude.text = "-----"
        } else {
            btnStartLocation.text = getString(R.string.stop_location_updates)
            btnStartLocation.setBackgroundColor(getColor(R.color.btn_off))
        }
    }

    private fun updateLocationScreen(latitude: Double, longitude: Double) {
        // Actualitza el text de pantalla amb les coordenades de localització obtingudes
        tvLatitude.text = String.format(Locale.getDefault(), "%.4f", latitude)
        tvLongitude.text = String.format(Locale.getDefault(), "%.4f", longitude)
    }
}