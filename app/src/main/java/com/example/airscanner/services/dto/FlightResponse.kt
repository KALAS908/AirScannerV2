package com.example.airscanner.services.dto

import android.os.Parcelable
import com.example.airscanner.models.Destination
import com.example.airscanner.models.Origin
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
