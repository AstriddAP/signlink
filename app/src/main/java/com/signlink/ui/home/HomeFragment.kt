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

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Botón de Pánico
        binding.cardPanic.setOnClickListener {
            viewModel.triggerPanicButton()
        }

        // Tarjeta 1: Alertas
        binding.cardAlerts.setOnClickListener {
            findNavController().navigate(R.id.nav_alerts)
        }

        // Tarjeta 2: Chat
        binding.cardCommunicate.setOnClickListener {
            findNavController().navigate(R.id.nav_communicate)
        }

        // Tarjeta 3: Ubicación
        binding.cardMap.setOnClickListener {
            findNavController().navigate(R.id.nav_map)
        }

        // Tarjeta 4: Subtítulos
        binding.cardCaptions.setOnClickListener {
            findNavController().navigate(R.id.nav_live_captioning)
        }

        // Tarjeta 5: Documentos
        binding.cardDocuments.setOnClickListener {
            findNavController().navigate(R.id.nav_documents)
        }

        binding.cardNotes.setOnClickListener {
            findNavController().navigate(R.id.nav_notes)
        }

        binding.cardAiExplanation.setOnClickListener {
            findNavController().navigate(R.id.nav_ai_explanation)
        }

        binding.cardNews.setOnClickListener {
            findNavController().navigate(R.id.nav_news)
        }

        binding.cardVideoCapture.setOnClickListener {
            findNavController().navigate(R.id.nav_video_capture)
        }
        
        binding.cardProfileImage.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.panicState.collectLatest { state ->
                when (state) {
                    is HomeViewModel.PanicState.Loading -> {
                        binding.cardPanic.isEnabled = false
                        Toast.makeText(context, "Enviando alerta...", Toast.LENGTH_SHORT).show()
                    }
                    is HomeViewModel.PanicState.Success -> {
                        binding.cardPanic.isEnabled = true
                        Toast.makeText(context, "¡ALERTA DE PÁNICO ENVIADA CON ÉXITO!", Toast.LENGTH_LONG).show()
                    }
                    is HomeViewModel.PanicState.Error -> {
                        binding.cardPanic.isEnabled = true
                        Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.cardPanic.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
