package com.signlink.ui.captioning

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signlink.databinding.FragmentAudioTranscriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AudioTranscriptionFragment : Fragment() {

    private var _binding: FragmentAudioTranscriptionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AudioTranscriptionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioTranscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriString = arguments?.getString("audio_uri")
        if (uriString != null) {
            viewModel.transcribeAudioUri(uriString.toUri())
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transcription.collectLatest { text ->
                binding.tvTranscriptionResult.text = text
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                binding.progressBar.isVisible = loading
                binding.btnSimplifyIa.isEnabled = !loading
            }
        }
    }

    private fun setupListeners() {
        binding.btnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Transcripción SignLink", binding.tvTranscriptionResult.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }

        binding.btnSimplifyIa.setOnClickListener {
            viewModel.simplifyWithIA()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
