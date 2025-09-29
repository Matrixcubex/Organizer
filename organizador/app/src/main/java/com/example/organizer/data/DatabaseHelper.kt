// app/src/main/java/com/example/organizer/data/DatabaseHelper.kt
// app/src/main/java/com/example/organizer/data/DatabaseHelper.kt
package com.example.organizer.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.organizer.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        internal const val DATABASE_NAME = "organizer.db"
        private const val DATABASE_VERSION = 2
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
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_EVENTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT NOT NULL,
                $KEY_TYPE TEXT NOT NULL,
                $KEY_CONTACT_NAME TEXT NOT NULL,
                $KEY_CONTACT_ID TEXT NOT NULL,
                $KEY_LOCATION_LAT REAL,
                $KEY_LOCATION_LNG REAL,
                $KEY_DESCRIPTION TEXT,
                $KEY_DATE TEXT NOT NULL,
                $KEY_TIME TEXT NOT NULL,
                $KEY_STATUS TEXT NOT NULL,
                $KEY_REMINDER TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL(
                    "ALTER TABLE $TABLE_EVENTS " +
                            "ADD COLUMN $KEY_STATUS TEXT DEFAULT 'Pendiente'"
                )
            } catch (e: android.database.sqlite.SQLiteException) {
                if (!e.message.orEmpty().contains("duplicate column name")) throw e
                // Si la columna ya existe, ignoramos y continuamos
            }
        }
    }

    // Resto del código CRUD permanece igual...
    // [Todas las demás funciones permanecen exactamente igual]


    // CRUD Operations
    fun addEvent(event: Event): Long {
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
            put(KEY_STATUS, event.status ?: "Pendiente")
            put(KEY_REMINDER, event.reminder)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun getEvents(): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_EVENTS,
            null, null, null, null, null, "$KEY_DATE ASC, $KEY_TIME ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                events.add(parseEventFromCursor(this))
            }
        }
        cursor.close()
        return events
    }

    fun getEventsByDate(date: String): List<Event> {
        return getEventsByDateRange(date, date)
    }

    fun getEventsByDateRange(startDate: String, endDate: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$KEY_DATE BETWEEN ? AND ?",
            arrayOf(startDate, endDate),
            null, null, "$KEY_DATE ASC, $KEY_TIME ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                events.add(parseEventFromCursor(this))
            }
        }
        cursor.close()
        return events
    }

    fun getEventsByMonth(month: Int, year: Int): List<Event> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)

        val startDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(calendar.time)

        return getEventsByDateRange(startDate, endDate)
    }

    fun getEventsByYear(year: Int): List<Event> {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1)

        val startDate = dateFormat.format(calendar.time)
        calendar.set(Calendar.MONTH, 11)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        val endDate = dateFormat.format(calendar.time)

        return getEventsByDateRange(startDate, endDate)
    }

    fun getEventsByType(type: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$KEY_TYPE = ?",
            arrayOf(type),
            null, null, "$KEY_DATE ASC, $KEY_TIME ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                events.add(parseEventFromCursor(this))
            }
        }
        cursor.close()
        return events
    }

    fun getEventsByStatus(status: String): List<Event> {
        val events = mutableListOf<Event>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$KEY_STATUS = ?",
            arrayOf(status),
            null, null, "$KEY_DATE ASC, $KEY_TIME ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                events.add(parseEventFromCursor(this))
            }
        }
        cursor.close()
        return events
    }

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
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$KEY_ID = ?",
            arrayOf(event.id.toString())
        )
    }

    fun updateEventStatus(id: Long, newStatus: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATUS, newStatus)
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun updateEventLocation(id: Long, lat: Double, lng: Double): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_LOCATION_LAT, lat)
            put(KEY_LOCATION_LNG, lng)
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun updateEventContact(id: Long, contactName: String, contactId: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_CONTACT_NAME, contactName)
            put(KEY_CONTACT_ID, contactId)
        }
        return db.update(
            TABLE_EVENTS,
            values,
            "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteEvent(id: Long): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_EVENTS,
            "$KEY_ID = ?",
            arrayOf(id.toString())
        )
    }

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