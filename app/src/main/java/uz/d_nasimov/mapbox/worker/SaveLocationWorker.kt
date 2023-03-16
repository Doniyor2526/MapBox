package uz.d_nasimov.mapbox.worker

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import uz.d_nasimov.mapbox.dao.LocationDao
import uz.d_nasimov.mapbox.database.AppDatabase
import uz.d_nasimov.mapbox.entity.Location
import java.util.concurrent.TimeUnit

class SaveLocationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var dao: LocationDao
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        dao = AppDatabase.getInstance(this.context).locationDao()

        if (checkPermissions()) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

            locationRequest = LocationRequest().apply {
                interval = TimeUnit.SECONDS.toMillis(60)
                fastestInterval = TimeUnit.SECONDS.toMillis(30)
                maxWaitTime = TimeUnit.MINUTES.toMillis(2)

                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {

                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let {
                        Log.d("TTTT", "onLocationResult: lat=${it.latitude}   lng=${it.longitude}")
                        dao.addLocation(Location(it.latitude, it.longitude, System.currentTimeMillis()))
                    }
                }
            }
            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
            }
        }
        return Result.retry()
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context, ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

}