package com.example.airscanner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FlightResponse(
    val callsign: String,
    val airlineName: String,
    val origin: Origin,
    val destination: Destination
) : Parcelable
{

}
