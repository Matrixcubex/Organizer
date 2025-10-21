// UbicacionActivity.kt - AGREGAR funcionalidad de rutas
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
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class UbicacionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var btnBack: Button
    private lateinit var etDireccion: EditText
    private lateinit var btnBuscar: Button
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var destino: String = ""
    private var tipo: String = ""
    private var ubicacionActual: LatLng? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val GOOGLE_DIRECTIONS_API_KEY = "AIzaSyDxSCysA9I_FyYYBsUXs0rRPmySqBjZfX8" // Tu API Key
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

        // Si viene destino del chatbot, establecerlo y buscar automáticamente
        if (destino.isNotEmpty()) {
            etDireccion.setText(destino)
            // Buscar automáticamente después de un pequeño delay
            etDireccion.postDelayed({
                buscarDireccionEnMapa(destino)
            }, 1000)
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

        // Si hay destino y ya tenemos ubicación actual, buscar ruta automáticamente
        if (destino.isNotEmpty() && ubicacionActual != null) {
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
                // Si hay destino, buscar ruta después de obtener permisos
                if (destino.isNotEmpty()) {
                    buscarDireccionEnMapa(destino)
                }
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
                ubicacionActual = LatLng(it.latitude, it.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacionActual!!)
                        .title("Tu ubicación actual")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual!!, 15f))

                Toast.makeText(this, "Ubicación actual obtenida", Toast.LENGTH_SHORT).show()

                // Si hay destino, calcular ruta automáticamente
                if (destino.isNotEmpty()) {
                    buscarDireccionEnMapa(destino)
                }
            } ?: run {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buscarDireccionEnMapa(direccion: String) {
        if (ubicacionActual == null) {
            Toast.makeText(this, "Obteniendo ubicación actual...", Toast.LENGTH_SHORT).show()
            mostrarUbicacionActual()
            return
        }

        // Por simplicidad, usaremos ubicaciones conocidas para demostración
        val ubicacionesConocidas = mapOf(
            "casa" to LatLng(19.4326, -99.1332), // CDMX
            "trabajo" to LatLng(19.4361, -99.1371),
            "hospital" to LatLng(19.4285, -99.1276),
            "escuela" to LatLng(19.4400, -99.1400),
            "supermercado" to LatLng(19.4350, -99.1300),
            "centro comercial" to LatLng(19.4380, -99.1350)
        )

        val ubicacionDestino = ubicacionesConocidas[direccion.lowercase()]
            ?: LatLng(19.4326, -99.1332) // Default: CDMX

        mMap.clear()

        // Marcador de ubicación actual
        mMap.addMarker(
            MarkerOptions()
                .position(ubicacionActual!!)
                .title("Tu ubicación")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )

        // Marcador de destino
        mMap.addMarker(
            MarkerOptions()
                .position(ubicacionDestino)
                .title(direccion)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        // Dibujar ruta (línea recta para demostración - en producción usarías Directions API)
        val ruta = PolylineOptions()
            .add(ubicacionActual!!, ubicacionDestino)
            .width(8f)
            .color(0xFFFF0000.toInt()) // Rojo
            .geodesic(true)

        mMap.addPolyline(ruta)

        // Ajustar cámara para mostrar ambos puntos
        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
            .include(ubicacionActual!!)
            .include(ubicacionDestino)
            .build()

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        Toast.makeText(this, "Ruta calculada hacia: $direccion", Toast.LENGTH_SHORT).show()

        // En una implementación real, aquí llamarías a la Google Directions API
        // calcularRutaConDirectionsAPI(ubicacionActual!!, ubicacionDestino)
    }

    // ✅ MÉTODO PARA RUTAS REALES CON GOOGLE DIRECTIONS API (OPCIONAL)
    private fun calcularRutaConDirectionsAPI(origen: LatLng, destino: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&key=$GOOGLE_DIRECTIONS_API_KEY"

                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                // Procesar la respuesta y dibujar la ruta en el mapa
                runOnUiThread {
                    procesarRespuestaDirections(jsonResponse)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UbicacionActivity, "Error al calcular ruta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun procesarRespuestaDirections(jsonResponse: JSONObject) {
        // Implementar el procesamiento de la respuesta de Directions API
        // Esto incluye decodificar el poliline y dibujar la ruta real
    }
}