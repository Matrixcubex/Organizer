// DatabaseHelper.kt - VERSIÓN COMPLETA CORREGIDA
package com.example.organizer.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.organizer.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        internal const val DATABASE_NAME = "organizer.db"
        private const val DATABASE_VERSION = 3 // Incrementar versión
        private const val TABLE_EVENTS = "events"

        // Columnas
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_TYPE = "type"
        const val KEY_CONTACT_NAME = "contactName"
        const val KEY_CONTACT_ID = "contactId"
        const val KEY_LOCATION_LAT = "locationLat"
        const val KEY_LOCATION_LNG = "locationLng"
        const val KEY_DESCRIPTION = "description"
        const val KEY_DATE = "date"
        const val KEY_TIME = "time"
        const val KEY_STATUS = "status"
        const val KEY_REMINDER = "reminder"
        const val KEY_IS_DAILY = "is_daily" // NUEVA COLUMNA para recordatorios diarios
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_EVENTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT NOT NULL,
                $KEY_TYPE TEXT NOT NULL,
                $KEY_CONTACT_NAME TEXT,
                $KEY_CONTACT_ID TEXT,
                $KEY_LOCATION_LAT REAL DEFAULT 0,
                $KEY_LOCATION_LNG REAL DEFAULT 0,
                $KEY_DESCRIPTION TEXT,
                $KEY_DATE TEXT NOT NULL,
                $KEY_TIME TEXT NOT NULL,
                $KEY_STATUS TEXT DEFAULT 'Pendiente',
                $KEY_REMINDER TEXT DEFAULT '30',
                $KEY_IS_DAILY INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTable)
        Log.d("DATABASE", "Tabla creada: $createTable")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $KEY_IS_DAILY INTEGER DEFAULT 0")
                Log.d("DATABASE", "Columna is_daily agregada")
            } catch (e: Exception) {
                Log.e("DATABASE", "Error al agregar columna: ${e.message}")
            }
        }
    }

    // ✅ AGREGAR EVENTO CORREGIDO
    fun addEvent(event: Event): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, event.title)
            put(KEY_TYPE, event.type)
            put(KEY_CONTACT_NAME, event.contactName ?: "")
            put(KEY_CONTACT_ID, event.contactId ?: "")
            put(KEY_LOCATION_LAT, event.locationLat)
            put(KEY_LOCATION_LNG, event.locationLng)
            put(KEY_DESCRIPTION, event.description ?: "")
            put(KEY_DATE, event.date)
            put(KEY_TIME, event.time)
            put(KEY_STATUS, event.status ?: "Pendiente")
            put(KEY_REMINDER, event.reminder ?: "30")
            put(KEY_IS_DAILY, if (event.date == "DIARIO") 1 else 0)
        }

        val id = db.insert(TABLE_EVENTS, null, values)
        Log.d("DATABASE", "Evento guardado - ID: $id, Título: ${event.title}, Tipo: ${event.type}")
        return id
    }

    // ✅ OBTENER TODOS LOS EVENTOS CORREGIDO
    fun getEvents(): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_EVENTS,
            null, null, null, null, null, "$KEY_DATE ASC, $KEY_TIME ASC"
        )

        Log.d("DATABASE", "Consultando eventos - Filas: ${cursor.count}")

        with(cursor) {
            while (moveToNext()) {
                try {
                    events.add(parseEventFromCursor(this))
                } catch (e: Exception) {
                    Log.e("DATABASE", "Error parseando evento: ${e.message}")
                }
            }
        }
        cursor.close()
        return events
    }

    // ✅ OBTENER EVENTOS POR FECHA CORREGIDO
    fun getEventsByDate(date: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase

        // Para eventos normales (fecha específica) Y recordatorios diarios
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$KEY_DATE = ? OR $KEY_IS_DAILY = 1",
            arrayOf(date),
            null, null, "$KEY_TIME ASC"
        )

        Log.d("DATABASE", "Eventos para $date - Encontrados: ${cursor.count}")

        with(cursor) {
            while (moveToNext()) {
                try {
                    events.add(parseEventFromCursor(this))
                } catch (e: Exception) {
                    Log.e("DATABASE", "Error parseando evento por fecha: ${e.message}")
                }
            }
        }
        cursor.close()
        return events
    }

    // ✅ OBTENER EVENTOS POR RANGO DE FECHAS
    fun getEventsByDateRange(startDate: String, endDate: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase

        // Convertir fechas para comparación
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)

            val allEvents = getEvents()
            events.addAll(allEvents.filter { event ->
                if (event.date == "DIARIO") {
                    true // Los recordatorios diarios siempre se muestran
                } else {
                    try {
                        val eventDate = dateFormat.parse(event.date)
                        eventDate in start..end
                    } catch (e: Exception) {
                        false
                    }
                }
            })

            Log.d("DATABASE", "Eventos en rango $startDate - $endDate: ${events.size}")
        } catch (e: Exception) {
            Log.e("DATABASE", "Error en getEventsByDateRange: ${e.message}")
        }

        return events
    }

    // ✅ OBTENER EVENTOS POR MES
    fun getEventsByMonth(month: Int, year: Int): List<Event> {
        val events = mutableListOf<Event>()
        val allEvents = getEvents()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        events.addAll(allEvents.filter { event ->
            if (event.date == "DIARIO") {
                true // Recordatorios diarios se muestran siempre
            } else {
                try {
                    val eventDate = dateFormat.parse(event.date)
                    val calendar = Calendar.getInstance().apply { time = eventDate }
                    calendar.get(Calendar.MONTH) == month && calendar.get(Calendar.YEAR) == year
                } catch (e: Exception) {
                    false
                }
            }
        })

        return events
    }

    // ✅ OBTENER EVENTOS POR AÑO
    fun getEventsByYear(year: Int): List<Event> {
        val events = mutableListOf<Event>()
        val allEvents = getEvents()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        events.addAll(allEvents.filter { event ->
            if (event.date == "DIARIO") {
                true // Recordatorios diarios se muestran siempre
            } else {
                try {
                    val eventDate = dateFormat.parse(event.date)
                    val calendar = Calendar.getInstance().apply { time = eventDate }
                    calendar.get(Calendar.YEAR) == year
                } catch (e: Exception) {
                    false
                }
            }
        })

        return events
    }

    // ✅ OBTENER EVENTOS POR TIPO
    fun getEventsByType(type: String): List<Event> {
        return getEvents().filter { it.type == type }
    }

    // ✅ OBTENER EVENTOS POR ESTADO
    fun getEventsByStatus(status: String): List<Event> {
        return getEvents().filter { it.status == status }
    }

    // ✅ ACTUALIZAR EVENTO
    fun updateEvent(event: Event): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, event.title)
            put(KEY_TYPE, event.type)
            put(KEY_CONTACT_NAME, event.contactName)
            put(KEY_CONTACT_ID, event.contactId)
            put(KEY_LOCATION_LAT, event.locationLat)
            put(KEY_LOCATION_LNG, event.locationLng)
            put(KEY_DESCRIPTION, event.description)
            put(KEY_DATE, event.date)
            put(KEY_TIME, event.time)
            put(KEY_STATUS, event.status)
            put(KEY_REMINDER, event.reminder)
            put(KEY_IS_DAILY, if (event.date == "DIARIO") 1 else 0)
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$KEY_ID = ?",
            arrayOf(event.id.toString())
        )
    }

    // ✅ ELIMINAR EVENTO
    fun deleteEvent(id: Long): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_EVENTS,
            "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    // ✅ PARSER DE EVENTOS MEJORADO
    private fun parseEventFromCursor(cursor: Cursor): Event {
        return Event(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
            type = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE)),
            contactName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NAME)),
            contactId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_ID)),
            locationLat = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LOCATION_LAT)),
            locationLng = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_LOCATION_LNG)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
            date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
            time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
            reminder = cursor.getString(cursor.getColumnIndexOrThrow(KEY_REMINDER))
        )
    }
}