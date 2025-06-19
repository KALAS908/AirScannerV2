package com.example.airscanner;

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.example.airscanner.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.io.InputStream

class MainActivity: AppCompatActivity(), OnMapReadyCallback  {

    private lateinit var gMap: GoogleMap
    private val airportMarkers = mutableListOf<Marker>()
    private lateinit var searchView: SearchView;
    private lateinit var SearchItem: String;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        searchView = findViewById(R.id.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { planeCode ->
                    searchPlaneByCode(planeCode.trim())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Optional: implement real-time search as user types
                return false
            }
        })



        val mapFragment = supportFragmentManager.findFragmentById(R.id.id_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap

        try {
            val inputStream: InputStream = assets.open("airport_coordinates.xlsx")

            val workbook: Workbook = XSSFWorkbook(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0)

            for (i in 1..sheet.lastRowNum) {
                val row: Row = sheet.getRow(i) ?: continue
                if (row.getCell(0) == null) continue

                val name = row.getCell(0).stringCellValue
                val latitude = row.getCell(1).numericCellValue
                val longitude = row.getCell(2).numericCellValue
                val elevation = row.getCell(3)?.toString() ?: ""
                val municipality = row.getCell(4)?.toString() ?: ""
                val country = row.getCell(5)?.toString() ?: ""
                val icao = row.getCell(6)?.toString() ?: ""
                val iata = row.getCell(7)?.toString() ?: ""

                val airport =
                    Airport(name, latitude, longitude, elevation, municipality, country, icao, iata)

                val marker = gMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitude, longitude))
                        .title(name)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.airport_marker))
                        .visible(false) // initially invisible
                )

                marker?.let {
                    it.tag = airport
                    airportMarkers.add(it)
                }
            }

            gMap.setOnCameraIdleListener {
                val zoom = gMap.cameraPosition.zoom
                val showMarkers = zoom > 6

                airportMarkers.forEach { marker ->
                    marker.isVisible = showMarkers
                }
            }

            gMap.setOnMarkerClickListener { marker ->
                val airport = marker.tag as? Airport
                airport?.let {
                    val info = """
                        ICAO: ${it.icao}
                        IATA: ${it.iata}
                        Municipality: ${it.municipality}
                        Country: ${it.country}
                        Elevation: ${it.elevation} ft
                    """.trimIndent()
                    marker.snippet = info
                    marker.showInfoWindow()
                }
                true
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Eroare la citirea fișierului", Toast.LENGTH_LONG).show()
        }
    }

    private fun searchPlaneByCode(planeCode: String) {
        // Show loading toast
        Toast.makeText(this, "Searching for $planeCode...", Toast.LENGTH_SHORT).show()

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("https://localhost:7123/") // Replace with your actual API base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(FlightApiService::class.java)
        val call = apiService.getFlightByCode(planeCode)

        call.enqueue(object : Callback<FlightResponse> {
            override fun onResponse(call: Call<FlightResponse>, response: Response<FlightResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val flightData = response.body()!!
                    showFlightOnMap(flightData)
                } else {
                    Toast.makeText(this@MainActivity, "Flight not found: $planeCode", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<FlightResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showFlightOnMap(flight: FlightResponse) {
        val flightPosition = LatLng(flight.latitude, flight.longitude)

        // Add marker for the searched flight
        val flightMarker = gMap.addMarker(
            MarkerOptions()
                .position(flightPosition)
                .title("Flight: ${flight.callsign}")
                .snippet("${flight.name} - ${flight.municipality}, ${flight.country}")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Different color for flights
        )

        // Move camera to flight location
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(flightPosition, 10f))

        // Show info window
        flightMarker?.showInfoWindow()

        // Success message
        Toast.makeText(this, "Found flight: ${flight.callsign}", Toast.LENGTH_SHORT).show()
    }


}
