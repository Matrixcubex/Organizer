// app/src/main/java/com/example/organizer/ui/about/AboutFragment.kt
package com.example.organizer.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.organizer.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.versionText.text = "Versión 1.0"
        binding.developerText.text = "Desarrollado por [Tu Nombre]"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}