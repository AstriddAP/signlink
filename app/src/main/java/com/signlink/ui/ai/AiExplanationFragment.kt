package com.signlink.ui.ai

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.signlink.R
import com.signlink.databinding.FragmentAiExplanationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class AiExplanationFragment : Fragment(R.layout.fragment_ai_explanation) {

    private var _binding: FragmentAiExplanationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiExplanationViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processSelectedFile(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAiExplanationBinding.bind(view)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSummarize.setOnClickListener {
            val text = binding.etInputText.text.toString()
            if (text.isNotBlank()) {
                viewModel.summarizeText(text)
            } else {
                showToast("Por favor ingresa un texto para resumir")
            }
        }

        binding.btnUploadFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
    }

    private fun processSelectedFile(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val fileName = getFileName(uri) ?: "upload_file"
            
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.summarizeFile(file)
        } catch (e: Exception) {
            showToast("Error al procesar el archivo")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = it.getString(nameIndex)
            }
        }
        return name
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
                        binding.tvSimplifiedResult.text = state.result
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
        binding.btnSummarize.isEnabled = enabled
        binding.btnUploadFile.isEnabled = enabled
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
