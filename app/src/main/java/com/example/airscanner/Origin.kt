package com.example.airscanner

data class Origin (
    val name: String,
    val municipality: String,
    val icaoCode: String?,
    val iataCode: String?,
    val latitude: Double,
    val longitude: Double
)
{
}