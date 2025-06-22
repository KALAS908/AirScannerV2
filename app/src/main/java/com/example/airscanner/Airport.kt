package com.example.airscanner;

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Airport(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: String,
    val municipality: String,
    val country: String,
    val icao: String,
    val iata: String
) : Parcelable