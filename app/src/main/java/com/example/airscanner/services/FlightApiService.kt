package com.example.airscanner.services

import com.example.airscanner.services.dto.FlightResponse
import com.example.airscanner.models.LiveFlight
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FlightApiService {
    @GET("Flight/{flightCode}")
    fun getFlightByCode(@Path("flightCode") flightCode: String): Call<FlightResponse>

    @GET("Flight/flights")
    fun getFlights(
        @Query("lamin") lamin: Double,
        @Query("lomin") lomin: Double,
        @Query("lamax") lamax: Double,
        @Query("lomax") lomax: Double
    ): Call<List<LiveFlight>>
}