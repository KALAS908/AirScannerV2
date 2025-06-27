package com.example.airscanner.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.airscanner.models.Airport
import com.example.airscanner.services.FlightApiService
import com.example.airscanner.fragments.FlightDetailsFragment
import com.example.airscanner.services.dto.FlightResponse
import com.example.airscanner.models.LiveFlight
import com.example.airscanner.R
import com.example.airscanner.activities.auth.LoginActivity
import com.example.airscanner.fragments.AirportDetailsFragment
import com.example.airscanner.models.TokenManager

data class TrackedFlight(
    val flight: LiveFlight,
    var marker: Marker,
    var lastUpdateTime: Long
)


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap
    private val trackedFlights = mutableMapOf<String, TrackedFlight>()
    private val airportMarkers = mutableListOf<Marker>()
    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        fragmentContainer?.setOnClickListener {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
        }

        val searchPanel = findViewById<LinearLayout>(R.id.search_mode_panel)
        val callsignSearch = findViewById<SearchView>(R.id.search_callsign)
        val logout = findViewById<ImageView>(R.id.btn_logout);


        logout.setOnClickListener {

            TokenManager.clearTokens()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<ImageView>(R.id.icon_search).setOnClickListener {
            searchPanel.visibility = View.VISIBLE
        }

        val closeSearchBtn = findViewById<ImageView>(R.id.btn_close_search)
        closeSearchBtn.setOnClickListener {
            searchPanel.visibility = View.GONE
        }

        callsignSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchPlaneByCode(it.trim())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?) = false
        })

        val mapFragment = supportFragmentManager.findFragmentById(R.id.id_map) as SupportMapFragment
        mapFragment.getMapAsync(this)



    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap

        val europeBounds = LatLngBounds(
            LatLng(35.0, -25.0),
            LatLng(71.0, 45.0)
        )

        gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(europeBounds, 0))
        gMap.setLatLngBoundsForCameraTarget(europeBounds)

        gMap.setMinZoomPreference(4f)
        gMap.setMaxZoomPreference(10f)

        loadAirports()

        gMap.setOnCameraIdleListener {
            val zoom = gMap.cameraPosition.zoom
            val showMarkers = zoom > 6
            airportMarkers.forEach { it.isVisible = showMarkers }
        }

        gMap.setOnMarkerClickListener { marker ->
            when (val tag = marker.tag) {
                is Airport -> {
                    showFragment(AirportDetailsFragment.newInstance(tag))
                }
                is String -> searchPlaneByCode(tag)
            }
            true
        }

         startAutoUpdate()
         startInterpolationLoop()
    }

    private fun loadAirports() {
        val minLat = 35.0
        val maxLat = 71.0
        val minLon = -25.0
        val maxLon = 45.0

        try {
            val inputStream: InputStream = assets.open("airport_coordinates.xlsx")
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                if (row.getCell(0) == null) continue

                val name = row.getCell(0).stringCellValue
                val latitude = row.getCell(1).numericCellValue
                val longitude = row.getCell(2).numericCellValue

                if (latitude !in minLat..maxLat || longitude !in minLon..maxLon) continue

                val airport = Airport(
                    name, latitude, longitude,
                    row.getCell(3)?.toString() ?: "",
                    row.getCell(4)?.toString() ?: "",
                    row.getCell(5)?.toString() ?: "",
                    row.getCell(6)?.toString() ?: "",
                    row.getCell(7)?.toString() ?: ""
                )

                val marker = gMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitude, longitude))
                        .title(name)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.airport_marker))
                        .visible(false)
                )
                marker?.tag = airport
                airportMarkers.add(marker!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Eroare la citirea fișierului", Toast.LENGTH_LONG).show()
        }
    }

    private fun startAutoUpdate() {
        handler = Handler(mainLooper)
        updateRunnable = object : Runnable {
            override fun run() {
                fetchFlights(35.0, -25.0, 71.0, 45.0) //roughly europe
                handler.postDelayed(this, 150_000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun searchPlaneByCode(planeCode: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://airscanner-h5d0bhehefe9h3cu.northeurope-01.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(FlightApiService::class.java)
        apiService.getFlightByCode(planeCode).enqueue(object : Callback<FlightResponse> {
            override fun onResponse(call: Call<FlightResponse>, response: Response<FlightResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val flight = response.body()!!
                    showFragment(FlightDetailsFragment.newInstance(flight))
                } else {
                    Toast.makeText(this@MainActivity, "Flight not found: $planeCode", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<FlightResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun fetchFlights(lamin: Double, lomin: Double, lamax: Double, lomax: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://airscanner-h5d0bhehefe9h3cu.northeurope-01.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(FlightApiService::class.java)
            .getFlights(lamin, lomin, lamax, lomax)
            .enqueue(object : Callback<List<LiveFlight>> {
                override fun onResponse(call: Call<List<LiveFlight>>, response: Response<List<LiveFlight>>) {
                    response.body()?.let {
                        drawFlightsOnMap(it)
                    }
                }

                override fun onFailure(call: Call<List<LiveFlight>>, t: Throwable) {}
            })
    }

    private fun drawFlightsOnMap(flights: List<LiveFlight>) {
        trackedFlights.values.forEach { it.marker.remove() }
        trackedFlights.clear()

        val now = System.currentTimeMillis()

        for (flight in flights) {
            val pos = LatLng(flight.latitude, flight.longitude)
            val track = (flight.trueTrack?.toFloat() ?: 0f)

            val icon = getScaledMarkerIcon(R.drawable.plane_icon_yellow, 0.25f)
            val marker = gMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(flight.callsign)
                    .icon(icon)
                    .rotation(track)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
            )

            if (marker != null && flight.callsign != null) {
                marker.tag = flight.callsign
                trackedFlights[flight.callsign] = TrackedFlight(flight, marker, now)
            }
        }
    }

    private fun getScaledMarkerIcon(resourceId: Int, scale: Float): BitmapDescriptor {
        val original = BitmapFactory.decodeResource(resources, resourceId)
        val width = (original.width * scale).toInt()
        val height = (original.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(original, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(scaled)
    }


    private fun showFragment(fragment: Fragment) {
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        if (fragmentContainer.visibility != View.VISIBLE) {
            fragmentContainer.visibility = View.VISIBLE
            fragmentContainer.translationX = fragmentContainer.width.toFloat()
            fragmentContainer.animate().translationX(0f).setDuration(300).start()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun startInterpolationLoop() {
        val interpolateHandler = Handler(mainLooper)
        interpolateHandler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                for ((_, tracked) in trackedFlights) {
                    val flight = tracked.flight
                    val dt = (now - tracked.lastUpdateTime) / 1000.0 // in seconds
                    val speedKnots = flight.velocity ?: continue
                    val headingDeg = flight.trueTrack ?: continue

                    val newPos = predictPosition(
                        tracked.flight.latitude,
                        tracked.flight.longitude,
                        speedKnots,
                        headingDeg,
                        dt
                    )

                    tracked.marker.position = newPos
                }

                interpolateHandler.postDelayed(this, 1000)
            }
        })
    }


    private fun predictPosition(lat: Double, lon: Double, speedKnots: Double, headingDegrees: Double, timeSeconds: Double): LatLng {
        val tune = 2.25
        val earthRadius = 6371.0 // km
        val speedKph = speedKnots * 1.852 * tune
        val distanceKm = speedKph * (timeSeconds / 3600.0)

        val headingRad = Math.toRadians(headingDegrees)
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        val newLat = Math.asin(
            Math.sin(latRad) * Math.cos(distanceKm / earthRadius) +
                    Math.cos(latRad) * Math.sin(distanceKm / earthRadius) * Math.cos(headingRad)
        )

        val newLon = lonRad + Math.atan2(
            Math.sin(headingRad) * Math.sin(distanceKm / earthRadius) * Math.cos(latRad),
            Math.cos(distanceKm / earthRadius) - Math.sin(latRad) * Math.sin(newLat)
        )

        return LatLng(Math.toDegrees(newLat), Math.toDegrees(newLon))
    }


    override fun onBackPressed() {
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        if (fragmentContainer.visibility == View.VISIBLE) {
            fragmentContainer.animate()
                .translationX(fragmentContainer.width.toFloat())
                .withEndAction {
                    fragmentContainer.visibility = View.GONE
                    supportFragmentManager.popBackStack()
                    fragmentContainer.translationX = 0f
                }
                .start()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}

