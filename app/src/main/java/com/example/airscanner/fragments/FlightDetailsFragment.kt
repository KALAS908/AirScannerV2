package com.example.airscanner.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.airscanner.services.dto.FlightResponse
import com.example.airscanner.R

class FlightDetailsFragment : Fragment() {

    private lateinit var flightResponse: FlightResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flightResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable("flightResponse", FlightResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable("flightResponse")
        } ?: throw IllegalStateException("flightResponse argument is missing")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_flight_response_details, container, false)

        val origin = flightResponse.origin
        val destination = flightResponse.destination

        view.findViewById<TextView>(R.id.text_callsign).text =
            "Callsign: ${flightResponse.callsign}"
        view.findViewById<TextView>(R.id.text_airline).text =
            "Airline: ${flightResponse.airlineName}"
        view.findViewById<TextView>(R.id.text_origin).text =
            "From: ${origin.name} (${origin.municipality})"
        view.findViewById<TextView>(R.id.text_destination).text =
            "To: ${destination.name} (${destination.municipality})"
        view.findViewById<TextView>(R.id.text_origin_coords).text =
            "Origin Coordinates: ${origin.latitude}, ${origin.longitude}"
        view.findViewById<TextView>(R.id.text_dest_coords).text =
            "Destination Coordinates: ${destination.latitude}, ${destination.longitude}"

        view.findViewById<ImageView>(R.id.btn_close).setOnClickListener {
            activity?.let { act ->
                val fragmentContainer = act.findViewById<FrameLayout>(R.id.fragment_container)
                fragmentContainer.animate()
                    .translationX(fragmentContainer.width.toFloat())
                    .withEndAction {
                        fragmentContainer.visibility = View.GONE
                        act.supportFragmentManager.popBackStack()
                        fragmentContainer.translationX = 0f
                    }
                    .start()
            }
        }

        return view
    }

    companion object {
        fun newInstance(flightResponse: FlightResponse): FlightDetailsFragment {
            val fragment = FlightDetailsFragment()
            val args = Bundle().apply {
                putParcelable("flightResponse", flightResponse)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
