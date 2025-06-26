package com.example.airscanner.models

data class LiveFlight (
    val icao24: String?,
    val callsign:String?,
    val originCountry: String?,
    val latitude: Double,
    val longitude: Double,
    val barolAltitude: String,
    val onGround: Boolean,
    val velocity: Double,
    val trueTrack: Double,
    val squawk: String,
    val category: String?,
)
