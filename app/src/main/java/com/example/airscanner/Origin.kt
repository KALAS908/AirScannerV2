package com.example.airscanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Origin (
    val name: String,
    val municipality: String,
    val icaoCode: String?,
    val iataCode: String?,
    val latitude: Double,
    val longitude: Double
) : Parcelable
{
}