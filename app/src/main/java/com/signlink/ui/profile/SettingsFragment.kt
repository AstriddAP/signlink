package com.signlink.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R
import com.signlink.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupUserData()
        setupListeners()
    }

    private fun setupUserData() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        binding.etName.setText(currentUser?.displayName ?: "Usuario")
        binding.etEmail.setText(currentUser?.email ?: "Sin correo")

        // Cargar estado guardado del modo oscuro
        val prefs = requireContext().getSharedPreferences("signlink_prefs", android.content.Context.MODE_PRIVATE)
        binding.switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireContext().getSharedPreferences("signlink_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("dark_mode", isChecked).apply()

            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.tvLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            // Regresar al inicio o al login
            requireActivity().finish()
            startActivity(requireActivity().intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
