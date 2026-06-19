package com.signlink.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.signlink.R
import com.signlink.data.model.Symbol
import com.signlink.databinding.FragmentHomeMudoBinding
import com.signlink.ui.communicate.SymbolAdapter
import com.signlink.util.TTSManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dashboard principal para el perfil de Discapacidad del Habla (Mudo).
 * Incluye acceso rápido a símbolos AAC y síntesis de voz.
 */
@AndroidEntryPoint
class HomeMudoFragment : Fragment(R.layout.fragment_home_mudo) {

    private var _binding: FragmentHomeMudoBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeMudoViewModel by viewModels()

    @Inject
    lateinit var ttsManager: TTSManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeMudoBinding.bind(view)
        
        setupGreeting()
        setupRecentPhrases()
        setupQuickSymbols()
        setupClickListeners()
    }

    private fun setupGreeting() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        binding.tvWelcomeMudo.text = getString(R.string.hello_user, currentUser?.displayName ?: "Usuario")
    }

    private fun setupRecentPhrases() {
        val recentPhrases = listOf(
            Symbol("r1", "Tengo sed", "Necesidades", "", "Tengo mucha sed"),
            Symbol("r2", "Gracias", "Social", "", "Muchas gracias por todo"),
            Symbol("r3", "Hola", "Social", "", "Hola, buenos días")
        )
        
        val adapter = SymbolAdapter { symbol ->
            speak(symbol.textToSpeak)
            binding.etQuickText.setText(symbol.textToSpeak)
        }
        
        binding.rvRecentPhrases.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRecentPhrases.adapter = adapter
        adapter.submitList(recentPhrases)
    }

    private fun setupQuickSymbols() {
        // 12 símbolos de uso frecuente
        val quickSymbols = listOf(
            Symbol("1", "Si", "Social", "", "Sí"),
            Symbol("2", "No", "Social", "", "No"),
            Symbol("3", "Ayuda", "Emergencia", "", "Necesito ayuda"),
            Symbol("4", "Baño", "Necesidades", "", "Quiero ir al baño"),
            Symbol("5", "Hambre", "Necesidades", "", "Tengo hambre"),
            Symbol("6", "Dolor", "Salud", "", "Siento dolor"),
            Symbol("7", "Familia", "Personas", "", "Llama a mi familia"),
            Symbol("8", "Agua", "Necesidades", "", "Quiero agua"),
            Symbol("9", "Bien", "Emociones", "", "Estoy bien"),
            Symbol("10", "Mal", "Emociones", "", "Me siento mal"),
            Symbol("11", "Casa", "Lugares", "", "Quiero ir a casa"),
            Symbol("12", "Médico", "Salud", "", "Necesito un médico")
        )

        val adapter = SymbolAdapter { symbol ->
            speak(symbol.textToSpeak)
            binding.etQuickText.setText(symbol.textToSpeak)
        }

        binding.rvQuickSymbols.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvQuickSymbols.adapter = adapter
        adapter.submitList(quickSymbols)
    }

    private fun setupClickListeners() {
        binding.btnSpeakMudo.setOnClickListener {
            val text = binding.etQuickText.text.toString()
            if (text.isNotEmpty()) {
                speak(text)
            } else {
                Toast.makeText(context, "Escribe algo primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabFullBoard.setOnClickListener {
            findNavController().navigate(R.id.nav_communicate)
        }

        binding.cardSettingsMudo.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
    }

    private fun speak(text: String) {
        ttsManager.speak(text)
        Log.d("HomeMudoFragment", "Speaking: $text")
    }

    override fun onPause() {
        super.onPause()
        ttsManager.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
