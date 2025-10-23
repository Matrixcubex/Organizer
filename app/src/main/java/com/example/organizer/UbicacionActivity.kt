package com.example.organizer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import android.graphics.Color

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
                // ✅ ACTUALIZAR ubicación antes de buscar
                actualizarUbicacionActual()
                // Pequeño delay para asegurar que tenemos la ubicación actual
                etDireccion.postDelayed({
                    buscarDireccionEnMapa(direccion)
                }, 500)
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

    // ✅ NUEVO MÉTODO: Actualizar ubicación actual
    private fun actualizarUbicacionActual() {
        if (!checkLocationPermission()) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                ubicacionActual = LatLng(it.latitude, it.longitude)
                Log.d("MAP_DEBUG", "Ubicación actualizada: $ubicacionActual")
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
            "centro comercial" to LatLng(19.4380, -99.1350),
            "mexico" to LatLng(19.4326, -99.1332),
            "cdmx" to LatLng(19.4326, -99.1332),
            "guadalajara" to LatLng(20.6597, -103.3496),
            "monterrey" to LatLng(25.6866, -100.3161)
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

        // ✅ CORREGIDO: Primero dibujar línea recta inmediatamente
        val rutaRecta = PolylineOptions()
            .add(ubicacionActual!!, ubicacionDestino)
            .width(6f)
            .color(Color.RED) // Rojo para línea recta
            .geodesic(true)

        mMap.addPolyline(rutaRecta)

        // ✅ CORREGIDO: LUEGO calcular ruta real con Directions API
        calcularRutaReal(ubicacionActual!!, ubicacionDestino)

        // Ajustar cámara para mostrar ambos puntos
        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
            .include(ubicacionActual!!)
            .include(ubicacionDestino)
            .build()

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        Toast.makeText(this, "Calculando ruta hacia: $direccion...", Toast.LENGTH_SHORT).show()
    }

    // ✅ MÉTODO PARA RUTAS REALES CON GOOGLE DIRECTIONS API
    private fun calcularRutaReal(origen: LatLng, destino: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving" + // Puedes cambiar a walking, transit, etc.
                        "&key=$GOOGLE_DIRECTIONS_API_KEY"

                Log.d("MAP_DEBUG", "Calculando ruta con URL: $url")

                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                Log.d("MAP_DEBUG", "Respuesta Directions API: ${jsonResponse.getString("status")}")

                if (jsonResponse.getString("status") == "OK") {
                    val routes = jsonResponse.getJSONArray("routes")
                    val route = routes.getJSONObject(0)
                    val legs = route.getJSONArray("legs")
                    val leg = legs.getJSONObject(0)

                    val distance = leg.getJSONObject("distance").getString("text")
                    val duration = leg.getJSONObject("duration").getString("text")

                    val polyline = route.getJSONObject("overview_polyline").getString("points")
                    val decodedPath = decodePolyline(polyline)

                    runOnUiThread {
                        dibujarRutaEnMapa(decodedPath, distance, duration)
                        Toast.makeText(this@UbicacionActivity, "Ruta calculada: $distance, $duration", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@UbicacionActivity, "No se pudo calcular la ruta: ${jsonResponse.getString("status")}", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("MAP_DEBUG", "Error al calcular ruta: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@UbicacionActivity, "Error al calcular ruta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    private fun dibujarRutaEnMapa(path: List<LatLng>, distance: String, duration: String) {
        // ✅ DIBUJAR RUTA REAL EN AZUL
        mMap.addPolyline(
            PolylineOptions()
                .addAll(path)
                .width(12f)
                .color(Color.BLUE)
                .geodesic(true)
        )

        // Agregar marcador informativo
        mMap.addMarker(
            MarkerOptions()
                .position(path.first())
                .title("Inicio - Distancia: $distance, Tiempo: $duration")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        Log.d("MAP_DEBUG", "Ruta dibujada: ${path.size} puntos, $distance, $duration")
    }

    private fun procesarRespuestaDirections(jsonResponse: JSONObject) {
        // Implementar el procesamiento de la respuesta de Directions API
        // Esto incluye decodificar el poliline y dibujar la ruta real
    }
}