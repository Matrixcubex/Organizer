// ContactPicker.kt
package com.example.organizer.utils

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class ContactPicker(private val fragment: Fragment) {
    private var onContactSelected: ((String, String) -> Unit)? = null

    private val contactPickerLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                fragment.requireActivity().contentResolver.query(
                    contactUri,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getString(0)
                        val name = cursor.getString(1)
                        onContactSelected?.invoke(name, id)
                    }
                }
            }
        }
    }

    fun pickContact(callback: (String, String) -> Unit) {
        onContactSelected = callback
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
        contactPickerLauncher.launch(intent)
    }
}