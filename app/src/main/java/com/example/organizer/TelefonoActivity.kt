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
        if (contactoBuscado.isNotEmpty() && contactosFiltrados.size == 1) {
            // Pequeño delay para que el usuario vea qué está pasando
            recyclerContactos.postDelayed({
                realizarLlamada(contactosFiltrados[0].telefono)
            }, 1000)
        }
    }

    private fun filtrarContactos(busqueda: String) {
        // VERIFICAR que contactAdapter esté inicializado
        if (!::contactAdapter.isInitialized) {
            return
        }

        contactosFiltrados.clear()

        if (busqueda.isEmpty()) {
            contactosFiltrados.addAll(listaContactos)
        } else {
            val busquedaLower = busqueda.lowercase()
            contactosFiltrados.addAll(
                listaContactos.filter {
                    it.nombre.lowercase().contains(busquedaLower) ||
                            it.telefono.contains(busqueda)
                }
            )
        }

        contactAdapter.notifyDataSetChanged()
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