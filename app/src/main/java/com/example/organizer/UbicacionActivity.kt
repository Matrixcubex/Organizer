package com.example.organizer
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class UbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var btnBack: Button
    private lateinit var etDireccion: EditText
    private lateinit var btnBuscar: Button
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var destino: String = ""
    private var tipo: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ubicacion)

        // Obtener parámetros del intent
        destino = intent.getStringExtra("DESTINO") ?: ""
        tipo = intent.getStringExtra("TIPO") ?: "navegacion"

        initViews()
        setupClickListeners()
        setupMap()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etDireccion = findViewById(R.id.et_direccion)
        btnBuscar = findViewById(R.id.btn_buscar)

        // Si viene destino del chatbot, establecerlo en el campo
        if (destino.isNotEmpty()) {
            etDireccion.setText(destino)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBuscar.setOnClickListener {
            val direccion = etDireccion.text.toString().trim()
            if (direccion.isNotEmpty()) {
                buscarDireccionEnMapa(direccion)
            } else {
                Toast.makeText(this, "Ingresa una dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurar mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Verificar permisos y mostrar ubicación actual
        if (checkLocationPermission()) {
            mostrarUbicacionActual()
        } else {
            solicitarPermisosUbicacion()
        }

        // Si hay destino, buscarlo
        if (destino.isNotEmpty()) {
            buscarDireccionEnMapa(destino)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisosUbicacion() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mostrarUbicacionActual()
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarUbicacionActual() {
        if (!checkLocationPermission()) return

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val ubicacionActual = LatLng(it.latitude, it.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacionActual)
                        .title("Tu ubicación actual")
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual, 15f))

                Toast.makeText(this, "Ubicación actual mostrada", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buscarDireccionEnMapa(direccion: String) {
        // Por simplicidad, usaremos una ubicación fija para demostración
        // En producción, usarías Geocoding API de Google Maps

        val ubicacionesConocidas = mapOf(
            "casa" to LatLng(19.4326, -99.1332), // CDMX
            "trabajo" to LatLng(19.4361, -99.1371),
            "hospital" to LatLng(19.4285, -99.1276),
            "escuela" to LatLng(19.4400, -99.1400)
        )

        val ubicacion = ubicacionesConocidas[direccion.lowercase()]
            ?: LatLng(19.4326, -99.1332) // Default: CDMX

        mMap.clear()
        mMap.addMarker(
            MarkerOptions()
                .position(ubicacion)
                .title(direccion)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f))

        Toast.makeText(this, "Mostrando: $direccion", Toast.LENGTH_SHORT).show()
    }
}