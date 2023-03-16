package uz.d_nasimov.mapbox

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import uz.d_nasimov.mapbox.databinding.ActivityMainBinding
import uz.d_nasimov.mapbox.worker.SaveLocationWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val requestCode = 3477

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkPermission()) {
            requestPermission()
        } else {
            initWorkManager()
        }
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,object :Style.OnStyleLoaded{
                override fun onStyleLoaded(style: Style) {
                    addAnnotationToMap()
                }

                private fun addAnnotationToMap() {
                    bitmapFromDrawableRes(
                        this@MainActivity,
                        R.drawable.taxi
                    ).let {
                        val annotationApi = binding.mapView.annotations
                        val pointAnnotationManager = annotationApi.createPointAnnotationManager(binding.mapView)

                        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()

                            .withPoint(Point.fromLngLat(18.06, 59.31))

                            .withIconImage(it!!)
                        pointAnnotationManager.create(pointAnnotationOptions)
                    }
                }
                private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
                    convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

            }
        )

        binding.mapView.location.updateSettings {
            locationPuck = LocationPuck2D(
                topImage = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.taxi
                )
            )
            enabled = true
            pulsingEnabled = true
        }

        if(AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_YES){
            val mapboxMap = binding.mapView.getMapboxMap()
            val STYLE_URL = Style.DARK
            mapboxMap.loadStyleUri(STYLE_URL)
        }
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) return true
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            requestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            binding.mapView.getMapboxMap().loadStyleUri(
                Style.MAPBOX_STREETS
            ) // After the style is loaded, initialize the Location component.
            {
                binding.mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
        }
    }

    fun initWorkManager() {
        val constraints =
            Constraints.Builder().build()

        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<SaveLocationWorker>(1, TimeUnit.MINUTES  )
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "save_user_location",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )

    }
}