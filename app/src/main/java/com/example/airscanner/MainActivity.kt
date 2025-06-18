package com.example.airscanner;

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.airscanner.R
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
import java.io.IOException
import java.io.InputStream

class MainActivity: AppCompatActivity(), OnMapReadyCallback  {

    private lateinit var gMap: GoogleMap
    private val airportMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
}