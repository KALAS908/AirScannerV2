package com.example.airscanner

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

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap: GoogleMap
    private val flightMarkers = mutableListOf<Marker>()
    private val airportMarkers = mutableListOf<Marker>()
    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        fragmentContainer?.setOnClickListener {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
        }

        val searchPanel = findViewById<LinearLayout>(R.id.search_mode_panel)
        val callsignSearch = findViewById<SearchView>(R.id.search_callsign)

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
    }

    private fun loadAirports() {
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
                fetchFlights(-90.0, -180.0, 90.0, 180.0) // toate zborurile
                handler.postDelayed(this, 30_000) // 30 secunde
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
        flightMarkers.forEach { it.remove() }
        flightMarkers.clear()

        flights.forEach { flight ->
            val position = LatLng(flight.latitude, flight.longitude)
            val marker = gMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(flight.callsign)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.plane_icon))
            )
            marker?.tag = flight.callsign
            marker?.let { flightMarkers.add(it) }
        }
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

