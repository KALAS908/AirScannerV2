package com.example.airscanner.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.airscanner.models.Airport
import com.example.airscanner.R

class AirportDetailsFragment : Fragment() {

    private lateinit var airport: Airport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        airport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable("airport", Airport::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable("airport")
        } ?: throw IllegalStateException("Airport argument is missing")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_airport_details, container, false)

        view.findViewById<TextView>(R.id.text_name).text = airport.name
        view.findViewById<TextView>(R.id.text_country).text = "Țară: ${airport.country}"
        view.findViewById<TextView>(R.id.text_municipality).text = "Municipiu: ${airport.municipality}"
        view.findViewById<TextView>(R.id.text_icao).text = "ICAO: ${airport.icao}"
        view.findViewById<TextView>(R.id.text_iata).text = "IATA: ${airport.iata}"
        view.findViewById<TextView>(R.id.text_elevation).text = "Altitudine: ${airport.elevation} ft"

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
        fun newInstance(airport: Airport): AirportDetailsFragment {
            val fragment = AirportDetailsFragment()
            val args = Bundle().apply {
                putParcelable("airport", airport)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
