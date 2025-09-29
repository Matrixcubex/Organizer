package com.example.organizer.utils

import android.content.Context
import com.example.organizer.data.DatabaseHelper

/**
 * Devuelve SIEMPRE la misma instancia de DatabaseHelper
 * para todo el proceso de la app.
 */
object DatabaseProvider {
    @Volatile private var INSTANCE: DatabaseHelper? = null

    fun get(context: Context): DatabaseHelper =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
        }
}
