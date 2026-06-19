package com.signlink.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.Priority
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getAndSendLocation()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

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

        // Cabecera: Emergencia Rápida (Comparte ubicación vía WhatsApp)
        binding.cardEmergency.setOnClickListener {
            sendEmergencyLocation()
        }

        // Tarjeta 2: Servicios Cercanos (abre Mapa de Servicios)
        binding.cardMap.setOnClickListener {
            findNavController().navigate(R.id.nav_map)
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

        // Tarjeta 6: Diccionario de Señas
        binding.cardDictionary.setOnClickListener {
            findNavController().navigate(R.id.nav_dictionary)
        }
    }

    private fun sendEmergencyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getAndSendLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getAndSendLocation() {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                shareLocationText(location)
            } else {
                // Si la última ubicación es nula, solicitamos una actualización en tiempo real
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).setMaxUpdates(1).build()
                
                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val lastLoc = result.lastLocation
                        if (lastLoc != null) {
                            shareLocationText(lastLoc)
                        } else {
                            Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual. Verifica tu GPS.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, android.os.Looper.getMainLooper())
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLocationText(location: Location) {
        val uri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val message = "¡EMERGENCIA! Esta es mi ubicación, tengo algún problema: $uri"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            val shareIntent = Intent.createChooser(intent, "Enviar ubicación de emergencia")
            startActivity(shareIntent)
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
