package com.example.organizer.utils
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

import android.content.Context
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import java.io.ByteArrayOutputStream
import java.io.File

class BackupUtils(
    private val context: Context,
    private val account: GoogleSignInAccount
) {
    private val driveService: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            // Garantiza un nombre de cuenta no-nulo
            selectedAccount = android.accounts.Account(account.email, "com.google")
        }

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Organizer").build()
    }

    fun backupToDrive() {
        try {
            val dbFile = context.getDatabasePath("organizer.db")
            if (!dbFile.exists()) {
                Toast.makeText(context, "No hay datos para respaldar", Toast.LENGTH_SHORT).show()
                return
            }

            val fileContent = dbFile.readBytes()
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "organizer_backup_${System.currentTimeMillis()}.db"
            }

            driveService.files().create(fileMetadata,
                com.google.api.client.http.ByteArrayContent("application/x-sqlite3", fileContent))
                .execute()

            Toast.makeText(context, "Respaldo exitoso", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error en respaldo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreFromDrive() {
        try {
            val result: FileList = driveService.files().list()
                .setQ("name contains 'organizer_backup'")
                .execute()

            result.files?.firstOrNull()?.let { file ->
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(file.id)
                    .executeMediaAndDownloadTo(outputStream)

                context.getDatabasePath("organizer.db").outputStream()
                    .use { it.write(outputStream.toByteArray()) }

                Toast.makeText(context, "Restauración exitosa", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(context, "No se encontraron respaldos", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error en restauración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}