package com.signlink.ui.ai

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.signlink.R
import com.signlink.databinding.FragmentAiExplanationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiExplanationFragment : Fragment(R.layout.fragment_ai_explanation) {

    private var _binding: FragmentAiExplanationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiExplanationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAiExplanationBinding.bind(view)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSimplify.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.simplifyText(text)
            } else {
                Toast.makeText(context, "Por favor ingresa un texto", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCorrect.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.correctText(text)
            } else {
                Toast.makeText(context, "Por favor ingresa un mensaje para corregir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDefine.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.defineWord(text)
            } else {
                Toast.makeText(context, "Escribe una palabra para definir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is AiExplanationViewModel.AiUiState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnSimplify.isEnabled = false
                        binding.btnCorrect.isEnabled = false
                        binding.btnDefine.isEnabled = false
                        binding.cardResult.isVisible = false
                    }
                    is AiExplanationViewModel.AiUiState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnSimplify.isEnabled = true
                        binding.btnCorrect.isEnabled = true
                        binding.btnDefine.isEnabled = true
                        binding.cardResult.isVisible = true
                        binding.tvSimplifiedResult.text = state.result
                    }
                    is AiExplanationViewModel.AiUiState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnSimplify.isEnabled = true
                        binding.btnCorrect.isEnabled = true
                        binding.btnDefine.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    AiExplanationViewModel.AiUiState.Idle -> {
                        binding.progressBar.isVisible = false
                        binding.btnSimplify.isEnabled = true
                        binding.btnCorrect.isEnabled = true
                        binding.btnDefine.isEnabled = true
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
