package com.signlink.ui.ai

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
                showToast("Por favor ingresa un texto")
            }
        }

        binding.btnCorrect.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.correctText(text)
            } else {
                showToast("Por favor ingresa un texto")
            }
        }

        binding.btnDefine.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.defineWord(text)
            } else {
                showToast("Por favor ingresa una palabra")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val isLoading = state is AiExplanationViewModel.AiUiState.Loading
                binding.progressBar.isVisible = isLoading
                setButtonsEnabled(!isLoading)

                when (state) {
                    is AiExplanationViewModel.AiUiState.Success -> {
                        binding.cardResult.isVisible = true
                        binding.tvAiResult.text = state.result
                    }
                    is AiExplanationViewModel.AiUiState.Error -> {
                        showToast(state.message)
                    }
                    AiExplanationViewModel.AiUiState.Idle -> {
                        binding.cardResult.isVisible = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnSimplify.isEnabled = enabled
        binding.btnCorrect.isEnabled = enabled
        binding.btnDefine.isEnabled = enabled
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
