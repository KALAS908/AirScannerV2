package com.example.airscanner

data class FlightResponse(
    val callsign: String,
    val airlineName: String,
    val origin: Origin,
    val destination: Destination
)
{

}
