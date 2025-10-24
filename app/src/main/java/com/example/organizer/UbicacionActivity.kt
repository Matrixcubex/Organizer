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
import com.google.android.gms.location.*
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
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var destino: String = ""
    private var tipo: String = ""
    private var ubicacionActual: LatLng? = null
    private var isCalculatingRoute: Boolean = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val GOOGLE_DIRECTIONS_API_KEY = "AIzaSyD0gWVjolSmCvDfdo5o3shDEjVALauKDDU"
        private const val LOCATION_UPDATE_INTERVAL = 5000L
        private const val FASTEST_LOCATION_INTERVAL = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ubicacion)

        destino = intent.getStringExtra("DESTINO") ?: ""
        tipo = intent.getStringExtra("TIPO") ?: "navegacion"

        Log.d("MAP_DEBUG", "📍 onCreate - Destino recibido: '$destino'")

        initViews()
        setupClickListeners()
        setupMap()
        setupLocationUpdates()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etDireccion = findViewById(R.id.et_direccion)
        btnBuscar = findViewById(R.id.btn_buscar)

        etDireccion.setText(destino)

        if (destino.isNotEmpty()) {
            Log.d("MAP_DEBUG", "📍 Destino no vacío, programando búsqueda automática")
            etDireccion.postDelayed({
                if (!isCalculatingRoute) {
                    buscarDireccionEnMapa(destino)
                }
            }, 1500)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBuscar.setOnClickListener {
            val nuevaDireccion = etDireccion.text.toString().trim()
            if (nuevaDireccion.isNotEmpty()) {
                Log.d("MAP_DEBUG", "📍 Buscando nueva dirección: '$nuevaDireccion'")
                destino = nuevaDireccion
                buscarDireccionEnMapa(nuevaDireccion)
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

    private fun setupLocationUpdates() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val nuevaUbicacion = LatLng(location.latitude, location.longitude)

                    if (ubicacionActual == null ||
                        distanciaEntre(ubicacionActual!!, nuevaUbicacion) > 50) {

                        ubicacionActual = nuevaUbicacion
                        Log.d("MAP_DEBUG", "📍 Ubicación actualizada: $ubicacionActual")

                        if (destino.isNotEmpty() && isCalculatingRoute) {
                            Log.d("MAP_DEBUG", "📍 Recalculando ruta por cambio de ubicación")
                            buscarDireccionEnMapa(destino)
                        }
                    }
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (checkLocationPermission()) {
            iniciarActualizacionesUbicacion()
            obtenerUbicacionActualInicial()
        } else {
            solicitarPermisosUbicacion()
        }

        if (destino.isNotEmpty()) {
            Log.d("MAP_DEBUG", "📍 onMapReady - Buscando destino: '$destino'")
            etDireccion.postDelayed({
                if (!isCalculatingRoute) {
                    buscarDireccionEnMapa(destino)
                }
            }, 2000)
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
                iniciarActualizacionesUbicacion()
                obtenerUbicacionActualInicial()
                if (destino.isNotEmpty()) {
                    buscarDireccionEnMapa(destino)
                }
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun iniciarActualizacionesUbicacion() {
        if (checkLocationPermission()) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
                Log.d("MAP_DEBUG", "📍 Iniciadas actualizaciones de ubicación")
            } catch (e: SecurityException) {
                Log.e("MAP_DEBUG", "📍 Error permisos ubicación: ${e.message}")
            }
        }
    }

    private fun obtenerUbicacionActualInicial() {
        if (!checkLocationPermission()) return

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                ubicacionActual = LatLng(it.latitude, it.longitude)
                Log.d("MAP_DEBUG", "📍 Ubicación inicial obtenida: $ubicacionActual")

                mMap.addMarker(
                    MarkerOptions()
                        .position(ubicacionActual!!)
                        .title("Tu ubicación actual")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual!!, 15f))

                Toast.makeText(this, "Ubicación actual obtenida", Toast.LENGTH_SHORT).show()

                if (destino.isNotEmpty() && !isCalculatingRoute) {
                    Log.d("MAP_DEBUG", "📍 Ubicación inicial - Buscando destino: '$destino'")
                    buscarDireccionEnMapa(destino)
                }
            } ?: run {
                Log.e("MAP_DEBUG", "📍 No se pudo obtener ubicación inicial")
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buscarDireccionEnMapa(direccion: String) {
        if (isCalculatingRoute) {
            Log.d("MAP_DEBUG", "📍 Ya hay un cálculo en curso, ignorando...")
            return
        }

        if (ubicacionActual == null) {
            Log.d("MAP_DEBUG", "📍 Ubicación actual nula, obteniendo...")
            Toast.makeText(this, "Obteniendo ubicación actual...", Toast.LENGTH_SHORT).show()
            obtenerUbicacionActualInicial()
            return
        }

        Log.d("MAP_DEBUG", "📍 Iniciando búsqueda para: '$direccion'")
        isCalculatingRoute = true

        // ✅ PRIMERO: Intentar con ubicaciones conocidas (para respuestas rápidas)
        val ubicacionesConocidas = mapOf(
            "casa" to LatLng(19.4326, -99.1332),
            "trabajo" to LatLng(19.4361, -99.1371),
            "hospital" to LatLng(19.4285, -99.1276),
            "escuela" to LatLng(19.4400, -99.1400),
            "supermercado" to LatLng(19.4350, -99.1300),
            "centro comercial" to LatLng(19.4380, -99.1350),
            "mexico" to LatLng(19.4326, -99.1332),
            "cdmx" to LatLng(19.4326, -99.1332),
            "guadalajara" to LatLng(20.6597, -103.3496),
            "monterrey" to LatLng(25.6866, -100.3161),
            "zocalo" to LatLng(19.4326, -99.1332),
            "aeropuerto" to LatLng(19.4363, -99.0721)
        )

        val ubicacionConocida = ubicacionesConocidas[direccion.lowercase()]

        if (ubicacionConocida != null) {
            Log.d("MAP_DEBUG", "📍 Usando ubicación conocida para: '$direccion'")
            procesarDestino(ubicacionConocida, direccion)
        } else {
            // ✅ SEGUNDO: Usar Geocoding API para cualquier dirección
            Log.d("MAP_DEBUG", "📍 Buscando con Geocoding API: '$direccion'")
            geocodificarDireccion(direccion)
        }
    }

    // ✅ NUEVO: Geocodificar cualquier dirección usando Google Geocoding API
    private fun geocodificarDireccion(direccion: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val direccionCodificada = java.net.URLEncoder.encode(direccion, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "address=$direccionCodificada" +
                        "&key=$GOOGLE_DIRECTIONS_API_KEY"

                Log.d("MAP_DEBUG", "📍 Geocoding URL: $url")

                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                Log.d("MAP_DEBUG", "📍 Geocoding Status: ${jsonResponse.getString("status")}")

                if (jsonResponse.getString("status") == "OK") {
                    val results = jsonResponse.getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val geometry = firstResult.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")

                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        val ubicacionDestino = LatLng(lat, lng)

                        val formattedAddress = firstResult.getString("formatted_address")

                        Log.d("MAP_DEBUG", "📍 Dirección encontrada: $formattedAddress ($lat, $lng)")

                        runOnUiThread {
                            procesarDestino(ubicacionDestino, formattedAddress)
                        }
                    } else {
                        throw Exception("No se encontraron resultados para la dirección")
                    }
                } else {
                    val errorMsg = jsonResponse.optString("error_message", "Error en geocoding")
                    throw Exception("Geocoding API error: ${jsonResponse.getString("status")} - $errorMsg")
                }

            } catch (e: Exception) {
                Log.e("MAP_DEBUG", "📍 Error en geocoding: ${e.message}")
                runOnUiThread {
                    // ✅ FALLBACK: Usar ubicación por defecto (CDMX)
                    Toast.makeText(this@UbicacionActivity, "⚠️ Usando ubicación por defecto para '$direccion'", Toast.LENGTH_LONG).show()
                    procesarDestino(LatLng(19.4326, -99.1332), "Ciudad de México")
                    isCalculatingRoute = false
                }
            }
        }
    }

    // ✅ NUEVO: Procesar destino (tanto para ubicaciones conocidas como geocodificadas)
    private fun procesarDestino(ubicacionDestino: LatLng, nombreDestino: String) {
        Log.d("MAP_DEBUG", "📍 Procesando destino: $nombreDestino en $ubicacionDestino")

        // Limpiar mapa
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
                .title(nombreDestino)
                .snippet("Destino")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        // Dibujar línea recta inmediatamente
        val rutaRecta = PolylineOptions()
            .add(ubicacionActual!!, ubicacionDestino)
            .width(6f)
            .color(Color.RED)
            .geodesic(true)

        mMap.addPolyline(rutaRecta)

        // Calcular ruta real con Directions API
        calcularRutaReal(ubicacionActual!!, ubicacionDestino, nombreDestino)

        // Ajustar cámara para mostrar ambos puntos
        val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
            .include(ubicacionActual!!)
            .include(ubicacionDestino)
            .build()

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        Toast.makeText(this, "Calculando ruta hacia: $nombreDestino...", Toast.LENGTH_SHORT).show()
    }

    // ✅ ACTUALIZADO: Ahora recibe el nombre del destino también
    private fun calcularRutaReal(origen: LatLng, destino: LatLng, nombreDestino: String) {
        Log.d("MAP_DEBUG", "📍 Calculando ruta real hacia: $nombreDestino")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving" +
                        "&key=$GOOGLE_DIRECTIONS_API_KEY"

                Log.d("MAP_DEBUG", "📍 URL Directions: $url")

                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                Log.d("MAP_DEBUG", "📍 Status Directions API: ${jsonResponse.getString("status")}")

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
                        dibujarRutaEnMapa(decodedPath, distance, duration, nombreDestino)
                        Toast.makeText(this@UbicacionActivity, "✅ Ruta a $nombreDestino: $distance, $duration", Toast.LENGTH_LONG).show()
                        isCalculatingRoute = false
                    }
                } else {
                    val errorMsg = jsonResponse.optString("error_message", "Error desconocido")
                    Log.e("MAP_DEBUG", "📍 Error Directions API: ${jsonResponse.getString("status")} - $errorMsg")

                    runOnUiThread {
                        Toast.makeText(this@UbicacionActivity, "⚠️ Usando ruta aproximada a $nombreDestino", Toast.LENGTH_LONG).show()
                        isCalculatingRoute = false
                    }
                }

            } catch (e: Exception) {
                Log.e("MAP_DEBUG", "📍 Error al calcular ruta: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@UbicacionActivity, "❌ Error de conexión para $nombreDestino", Toast.LENGTH_SHORT).show()
                    isCalculatingRoute = false
                }
            }
        }
    }

    private fun distanciaEntre(punto1: LatLng, punto2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            punto1.latitude, punto1.longitude,
            punto2.latitude, punto2.longitude,
            results
        )
        return results[0]
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

    // ✅ ACTUALIZADO: Mostrar nombre del destino en el log
    private fun dibujarRutaEnMapa(path: List<LatLng>, distance: String, duration: String, destino: String) {
        mMap.addPolyline(
            PolylineOptions()
                .addAll(path)
                .width(12f)
                .color(Color.BLUE)
                .geodesic(true)
        )

        Log.d("MAP_DEBUG", "📍 Ruta dibujada a $destino: ${path.size} puntos, $distance, $duration")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("MAP_DEBUG", "📍 Actualizaciones de ubicación detenidas")
        } catch (e: Exception) {
            Log.e("MAP_DEBUG", "📍 Error deteniendo ubicación: ${e.message}")
        }
    }
}