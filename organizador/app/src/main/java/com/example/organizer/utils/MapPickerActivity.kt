package com.example.organizer.utils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.organizer.R
import com.example.organizer.databinding.DialogMapPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: DialogMapPickerBinding
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el SupportMapFragment y notificar cuando el mapa esté listo
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.confirmButton.setOnClickListener {
            selectedLocation?.let { location ->
                val resultIntent = Intent().apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configuración inicial del mapa
        val defaultLocation = LatLng(19.4326, -99.1332) // Ejemplo: CDMX
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Listener para clicks en el mapa
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng))
            selectedLocation = latLng
        }
    }
}