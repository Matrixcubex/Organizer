// TelefonoActivity.kt - VERSIÓN CORREGIDA
package com.example.organizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizer.data.ContactAdapter
import com.example.organizer.data.model.Contact
import android.util.Log
class TelefonoActivity : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var etBuscarContacto: EditText
    private lateinit var btnBuscar: Button
    private lateinit var recyclerContactos: RecyclerView

    // INICIALIZAR DIRECTAMENTE en lugar de usar lateinit
    private lateinit var contactAdapter: ContactAdapter

    private var contactoBuscado: String = ""
    private var listaContactos = mutableListOf<Contact>()
    private var contactosFiltrados = mutableListOf<Contact>()

    companion object {
        private const val PERMISSION_REQUEST_READ_CONTACTS = 1002
        private const val PERMISSION_REQUEST_CALL_PHONE = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telefono)

        // Obtener parámetros del intent
        contactoBuscado = intent.getStringExtra("CONTACTO_BUSCADO") ?: ""

        initViews()
        setupRecyclerView() // ← MOVER ESTO ANTES de setupClickListeners
        setupClickListeners()

        // Verificar permisos y cargar contactos
        if (checkContactsPermission()) {
            cargarContactos()
        } else {
            solicitarPermisosContactos()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etBuscarContacto = findViewById(R.id.et_buscar_contacto)
        recyclerContactos = findViewById(R.id.recycler_contactos)
        btnBuscar = findViewById(R.id.btn_buscar_contacto)

        // Si viene contacto del chatbot, establecerlo en el campo de búsqueda
        if (contactoBuscado.isNotEmpty()) {
            etBuscarContacto.setText(contactoBuscado)
            // NO llamar filtrarContactos aquí todavía
        }
    }

    private fun setupRecyclerView() {
        // INICIALIZAR contactAdapter aquí
        contactAdapter = ContactAdapter(contactosFiltrados) { contacto ->
            realizarLlamada(contacto.telefono)
        }

        recyclerContactos.apply {
            layoutManager = LinearLayoutManager(this@TelefonoActivity)
            adapter = contactAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBuscar.setOnClickListener {
            val busqueda = etBuscarContacto.text.toString().trim()
            filtrarContactos(busqueda)
        }
    }

    private fun checkContactsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCallPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisosContactos() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            PERMISSION_REQUEST_READ_CONTACTS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cargarContactos()
                } else {
                    Toast.makeText(this, "Se necesitan permisos para acceder a contactos", Toast.LENGTH_LONG).show()
                }
            }
            PERMISSION_REQUEST_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permiso concedido, intenta llamar de nuevo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Se necesitan permisos para realizar llamadas", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargarContactos() {
        listaContactos.clear()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone._ID
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (c.moveToNext()) {
                val nombre = c.getString(nameIndex)
                val telefono = c.getString(numberIndex)

                // Filtrar contactos sin número
                if (telefono != null && telefono.isNotEmpty()) {
                    listaContactos.add(Contact(nombre, telefono))
                }
            }
        }

        // Actualizar lista filtrada
        val busquedaActual = etBuscarContacto.text.toString().trim()
        filtrarContactos(busquedaActual)

        Toast.makeText(this, "${listaContactos.size} contactos cargados", Toast.LENGTH_SHORT).show()

        // Si hay búsqueda específica del chatbot y encontramos un contacto, llamar automáticamente
        // Si hay búsqueda específica del chatbot y encontramos contactos
        if (contactoBuscado.isNotEmpty()) {
            Log.d("AUTO_CALL", "Búsqueda automática para: '$contactoBuscado'")
            Log.d("AUTO_CALL", "Contactos encontrados: ${contactosFiltrados.size}")

            if (contactosFiltrados.size == 1) {
                // Solo un contacto encontrado - llamar automáticamente
                val contacto = contactosFiltrados[0]
                Log.d("AUTO_CALL", "Llamando automáticamente a: ${contacto.nombre} - ${contacto.telefono}")

                recyclerContactos.postDelayed({
                    realizarLlamada(contacto.telefono)
                }, 1500)
            } else if (contactosFiltrados.size > 1) {
                // Múltiples contactos - mostrar toast
                Toast.makeText(this,
                    "${contactosFiltrados.size} contactos encontrados. Selecciona uno.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Ningún contacto encontrado
                Toast.makeText(this,
                    "No se encontró el contacto '$contactoBuscado'",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun filtrarContactos(busqueda: String) {
        if (!::contactAdapter.isInitialized) {
            return
        }

        contactosFiltrados.clear()

        if (busqueda.isEmpty()) {
            contactosFiltrados.addAll(listaContactos)
        } else {
            val busquedaLower = busqueda.lowercase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")

            contactosFiltrados.addAll(
                listaContactos.filter { contacto ->
                    // Buscar en nombre (sin acentos)
                    val nombreLower = contacto.nombre.lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")

                    // Buscar en teléfono (solo números)
                    val telefonoLimpio = contacto.telefono.replace(Regex("[^0-9]"), "")
                    val busquedaNumeros = busqueda.replace(Regex("[^0-9]"), "")

                    // Coincidencia parcial en nombre O coincidencia en teléfono
                    nombreLower.contains(busquedaLower) ||
                            contacto.nombre.lowercase().contains(busquedaLower) ||
                            telefonoLimpio.contains(busquedaNumeros)
                }
            )
        }

        contactAdapter.notifyDataSetChanged()

        // DEBUG: Mostrar cuántos contactos encontró
        Log.d("CONTACT_SEARCH", "Búsqueda: '$busqueda' - Encontrados: ${contactosFiltrados.size}")

        // Mostrar los contactos encontrados para debug
        contactosFiltrados.forEachIndexed { index, contacto ->
            Log.d("CONTACT_FOUND", "$index: ${contacto.nombre} - ${contacto.telefono}")
        }
    }

    private fun realizarLlamada(numeroTelefono: String) {
        if (!checkCallPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_REQUEST_CALL_PHONE
            )
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$numeroTelefono")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error: Permiso de llamada denegado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al realizar la llamada: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}