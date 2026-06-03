package com.signlink.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.signlink.R
import com.signlink.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        binding.tvWelcomeTitle.text = getString(R.string.hello_user, currentUser?.displayName ?: "Usuario")
        
        // Texto fijo para el modo unificado
        binding.tvProfileMode.text = "Panel de Herramientas de Accesibilidad"
    }

    private fun setupClickListeners() {
        // Tarjeta 1: Chat
        binding.cardCommunicate.setOnClickListener {
            findNavController().navigate(R.id.nav_communicate)
        }

        // Tarjeta 2: Emergencia
        binding.cardEmergency.setOnClickListener {
            sendEmergencyLocation()
        }

        // Tarjeta 3: Modo Escucha
        binding.cardCaptions.setOnClickListener {
            findNavController().navigate(R.id.nav_live_captioning)
        }

        // Tarjeta 4: Transcriptor WhatsApp
        binding.cardAudioTranscriber.setOnClickListener {
            findNavController().navigate(R.id.nav_audio_transcription)
        }

        // Tarjeta 5: IA Explica
        binding.cardAiExplanation.setOnClickListener {
            findNavController().navigate(R.id.nav_ai_explanation)
        }

        // Tarjeta 6: Diccionario LSP
        binding.cardVisualDictionary.setOnClickListener {
            findNavController().navigate(R.id.nav_visual_dictionary)
        }
        
        binding.cardProfileImage.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
    }

    private fun sendEmergencyLocation() {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val uri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    val message = "¡EMERGENCIA! Necesito ayuda. Mi ubicación actual es: $uri"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.setPackage("com.whatsapp")
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, message)
                    
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
                        // Fallback sharing
                        val shareIntent = android.content.Intent.createChooser(intent, "Enviar ubicación de emergencia")
                        startActivity(shareIntent)
                    }
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación. Verifica tu GPS.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // No Panic Button to observe
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
