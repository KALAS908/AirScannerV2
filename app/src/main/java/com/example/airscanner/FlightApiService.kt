package com.example.airscanner

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface FlightApiService {
    @GET("Flight/{flightCode}")
    fun getFlightByCode(@Path("flightCode") flightCode: String): Call<FlightResponse>
}