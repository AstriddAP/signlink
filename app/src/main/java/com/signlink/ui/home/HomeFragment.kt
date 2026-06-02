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

        binding.cardVideoCapture.setOnClickListener {
            findNavController().navigate(R.id.nav_video_capture)
        }
        
        binding.cardProfileImage.setOnClickListener {
            findNavController().navigate(R.id.nav_settings)
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
