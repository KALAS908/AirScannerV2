package com.example.airscanner

data class Destination (val name: String,
                        val municipality: String,
                        val country: String,
                        val icaoCode: String?,
                        val iataCode: String?,
                        val latitude: Double,
                        val longitude: Double)
