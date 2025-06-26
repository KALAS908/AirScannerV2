package com.example.airscanner.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Destination (val name: String,
                        val municipality: String,
                        val country: String,
                        val icaoCode: String?,
                        val iataCode: String?,
                        val latitude: Double,
                        val longitude: Double) : Parcelable
