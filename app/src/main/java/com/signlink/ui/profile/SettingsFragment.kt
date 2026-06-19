package com.signlink.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.signlink.R
import com.signlink.databinding.FragmentSettingsBinding
import com.signlink.util.TTSManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var ttsManager: TTSManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupUserData()
        setupListeners()
        setupCustomSettings()
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

    private fun setupCustomSettings() {
        val prefs = requireContext().getSharedPreferences("signlink_prefs", Context.MODE_PRIVATE)

        // 1. Tono de Voz (Pitch)
        val pitchOptions = listOf("Voz Grave (Masculino)", "Voz Predeterminada", "Voz Aguda (Femenino)")
        val pitchValues = listOf(0.75f, 1.0f, 1.3f)
        val pitchAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pitchOptions)
        pitchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVoicePitch.adapter = pitchAdapter

        val savedPitch = prefs.getFloat("voice_pitch", 1.0f)
        val pitchIndex = pitchValues.indexOf(savedPitch).let { if (it == -1) 1 else it }
        binding.spinnerVoicePitch.setSelection(pitchIndex)

        binding.spinnerVoicePitch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putFloat("voice_pitch", pitchValues[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Velocidad de Habla (Speech Rate)
        val speedOptions = listOf("Velocidad Lenta", "Velocidad Normal", "Velocidad Rápida")
        val speedValues = listOf(0.75f, 1.0f, 1.25f)
        val speedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, speedOptions)
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVoiceSpeed.adapter = speedAdapter

        val savedSpeed = prefs.getFloat("voice_speed", 1.0f)
        val speedIndex = speedValues.indexOf(savedSpeed).let { if (it == -1) 1 else it }
        binding.spinnerVoiceSpeed.setSelection(speedIndex)

        binding.spinnerVoiceSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putFloat("voice_speed", speedValues[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3. Modo de Resumen IA
        val modeOptions = listOf("Resumen Sencillo (IA más sencillo)", "Resumen Estándar", "Resumen Técnico / Detallado")
        val modeValues = listOf("sencillo", "estandar", "tecnico")
        val modeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modeOptions)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSummaryMode.adapter = modeAdapter

        val savedMode = prefs.getString("summary_mode", "sencillo") ?: "sencillo"
        val modeIndex = modeValues.indexOf(savedMode).let { if (it == -1) 0 else it }
        binding.spinnerSummaryMode.setSelection(modeIndex)
        updateSummaryModeDesc(savedMode)

        binding.spinnerSummaryMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMode = modeValues[position]
                prefs.edit().putString("summary_mode", selectedMode).apply()
                updateSummaryModeDesc(selectedMode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateSummaryModeDesc(mode: String) {
        val desc = when (mode) {
            "sencillo" -> "Usa lenguaje sumamente sencillo, oraciones breves y emojis visuales. Diseñado especialmente para personas sordas que prefieren evitar tecnicismos."
            "tecnico" -> "Resumen técnico de alto nivel. Conserva terminología formal, conceptos especializados, métricas y datos precisos."
            else -> "Resumen estándar y equilibrado. Ideal para una lectura ágil manteniendo los puntos clave principales."
        }
        binding.tvSummaryModeDesc.text = desc
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

