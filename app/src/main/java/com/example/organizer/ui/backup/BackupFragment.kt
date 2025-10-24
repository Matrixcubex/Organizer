// app/src/main/java/com/example/organizer/ui/backup/BackupFragment.kt
package com.example.organizer.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.organizer.databinding.FragmentBackupBinding
import com.example.organizer.utils.BackupUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import java.io.File

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveAccount: GoogleSignInAccount? = null
    private lateinit var backupUtils: BackupUtils

    // Google Sign-In launcher
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            driveAccount = task.getResult(ApiException::class.java)
            initializeBackupUtils()
            enableDriveButtons(true)
            updateDriveStatus("Conectado a Google Drive")
            Toast.makeText(requireContext(), "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            handleSignInError(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de Google Sign-In compatible con las dependencias
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Verificar si ya hay una sesión activa
        driveAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        driveAccount?.let {
            initializeBackupUtils()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        updateDriveStatus(if (driveAccount != null) "Conectado a Google Drive" else "No conectado")
    }

    private fun setupUI() {
        // Configurar estado inicial de los botones
        enableDriveButtons(driveAccount != null)

        // Google Drive - listeners
        binding.btnBackup.setOnClickListener {
            handleBackupToDrive()
        }

        binding.btnRestore.setOnClickListener {
            handleRestoreFromDrive()
        }

        // Long click para iniciar sesión
        binding.btnBackup.setOnLongClickListener {
            signInToDrive()
            true
        }

        binding.btnRestore.setOnLongClickListener {
            signInToDrive()
            true
        }

        // Backup local
        binding.btnLocalBackup.setOnClickListener { createLocalBackup() }
        binding.btnLocalRestore.setOnClickListener { restoreInternalBackup() }

        // Botón de logout
        binding.btnLogout.setOnClickListener { signOutFromDrive() }
    }

    private fun handleBackupToDrive() {
        if (driveAccount != null) {
            try {
                backupUtils.backupToDrive()
                Toast.makeText(requireContext(), "Iniciando backup a Google Drive...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error en backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Primero inicia sesión en Google Drive", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleRestoreFromDrive() {
        if (driveAccount != null) {
            try {
                backupUtils.restoreFromDrive()
                Toast.makeText(requireContext(), "Iniciando restauración desde Google Drive...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error en restauración: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Primero inicia sesión en Google Drive", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeBackupUtils() {
        driveAccount?.let {
            backupUtils = BackupUtils(requireContext(), it)
        }
    }

    private fun signInToDrive() {
        try {
            signInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al iniciar sesión: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun signOutFromDrive() {
        googleSignInClient.signOut().addOnCompleteListener {
            driveAccount = null
            enableDriveButtons(false)
            updateDriveStatus("Sesión cerrada")
            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableDriveButtons(enabled: Boolean) {
        binding.btnBackup.isEnabled = enabled
        binding.btnRestore.isEnabled = enabled
        binding.btnLogout.isEnabled = enabled
    }

    private fun updateDriveStatus(status: String) {
        binding.tvDriveStatus.text = status
    }

    private fun handleSignInError(exception: ApiException) {
        val errorMessage = when (exception.statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Inicio de sesión cancelado"
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Error en inicio de sesión"
            else -> "Error desconocido: ${exception.statusCode}"
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        enableDriveButtons(false)
        updateDriveStatus("Error de conexión")
    }

    /* ---------- Local backup helpers ---------- */

    /** Copia organizer.db → files/organizer_backup.db */
    private fun createLocalBackup() {
        try {
            val dbFile = requireContext().getDatabasePath("organizer.db")
            if (!dbFile.exists()) {
                Toast.makeText(requireContext(), "No hay datos para respaldar", Toast.LENGTH_SHORT).show()
                return
            }

            val dest = File(requireContext().filesDir, "organizer_backup.db")
            dbFile.copyTo(dest, overwrite = true)

            Toast.makeText(requireContext(),
                "Backup local creado exitosamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "Error al crear backup local: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Copia files/organizer_backup.db → databases/organizer.db */
    private fun restoreInternalBackup() {
        try {
            val src = File(requireContext().filesDir, "organizer_backup.db")
            if (!src.exists()) {
                Toast.makeText(requireContext(),
                    "No se encontró ningún backup interno", Toast.LENGTH_SHORT).show()
                return
            }

            val dbFile = requireContext().getDatabasePath("organizer.db")
            src.copyTo(dbFile, overwrite = true)

            Toast.makeText(requireContext(),
                "Restauración local exitosa", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "Error al restaurar backup local: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Clase interna para códigos de estado de Google Sign-In
    companion object GoogleSignInStatusCodes {
        const val SIGN_IN_CANCELLED = 12501
        const val SIGN_IN_FAILED = 12502
    }
}