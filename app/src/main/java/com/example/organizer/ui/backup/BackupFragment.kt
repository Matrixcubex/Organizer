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
import java.io.File

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private lateinit var gsc: GoogleSignInClient
    private var driveAccount: GoogleSignInAccount? = null
    private val backupUtils: BackupUtils by lazy {
        BackupUtils(requireContext(), driveAccount!!)
    }

    // --- Google Sign-In launcher ---
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            driveAccount = task.getResult(ApiException::class.java)
            enableDriveButtons(true)
        } catch (e: ApiException) {
            Toast.makeText(requireContext(), "Login cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.drive.Drive.SCOPE_FILE)
            .build()

        gsc = GoogleSignIn.getClient(requireContext(), gso)
        driveAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        enableDriveButtons(driveAccount != null)

        // --- Google Drive ---
        binding.btnBackup.setOnClickListener   { backupUtils.backupToDrive() }
        binding.btnRestore.setOnClickListener { backupUtils.restoreFromDrive() }
        binding.btnBackup.setOnLongClickListener   { signInToDrive(); true }
        binding.btnRestore.setOnLongClickListener { signInToDrive(); true }

        // --- Backup local ---
        binding.btnLocalBackup.setOnClickListener  { createLocalBackup() }
        binding.btnLocalRestore.setOnClickListener { restoreInternalBackup() }
    }

    private fun signInToDrive() {
        signInLauncher.launch(gsc.signInIntent)
    }

    private fun enableDriveButtons(enabled: Boolean) {
        binding.btnBackup.isEnabled  = enabled
        binding.btnRestore.isEnabled = enabled
    }

    /* ---------- Local backup helpers ---------- */

    /** Copia organizer.db → files/organizer_backup.db */
    private fun createLocalBackup() {
        val dbFile = requireContext().getDatabasePath("organizer.db")
        if (!dbFile.exists()) {
            Toast.makeText(requireContext(), "No hay datos para respaldar", Toast.LENGTH_SHORT).show()
            return
        }
        val dest = File(requireContext().filesDir, "organizer_backup.db")
        dbFile.copyTo(dest, overwrite = true)
        Toast.makeText(requireContext(),
            "Backup local creado en ${dest.path}", Toast.LENGTH_SHORT).show()
    }

    /** Copia files/organizer_backup.db → databases/organizer.db */
    private fun restoreInternalBackup() {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
