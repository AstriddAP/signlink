package com.signlink.ui.communicate

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.signlink.R
import com.signlink.data.model.Symbol
import com.signlink.databinding.FragmentCommunicateBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class CommunicateFragment : Fragment(R.layout.fragment_communicate), TextToSpeech.OnInitListener {
    private var _binding: FragmentCommunicateBinding? = null
    private val binding get() = _binding!!
    private var tts: TextToSpeech? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunicateBinding.bind(view)
        
        tts = TextToSpeech(requireContext(), this)
        setupSymbolsGrid()

        binding.btnSpeak.setOnClickListener {
            val text = binding.etMessage.text.toString()
            speak(text)
        }
    }

    private fun setupSymbolsGrid() {
        val mockupSymbols = listOf(
            Symbol("1", "Hola", "Social", "", "Hola, ¿cómo estás?"),
            Symbol("2", "Gracias", "Social", "", "Muchas gracias"),
            Symbol("3", "Ayuda", "Emergencia", "", "Necesito ayuda, por favor"),
            Symbol("4", "Hambre", "Necesidades", "", "Tengo hambre"),
            Symbol("5", "Sed", "Necesidades", "", "Tengo sed"),
            Symbol("6", "Baño", "Necesidades", "", "¿Dónde está el baño?"),
            Symbol("7", "Dolor", "Salud", "", "Me duele algo"),
            Symbol("8", "Bien", "Emociones", "", "Estoy bien"),
            Symbol("9", "Mal", "Emociones", "", "Me siento mal")
        )

        val adapter = SymbolAdapter { symbol ->
            speak(symbol.textToSpeak)
            binding.etMessage.setText(symbol.textToSpeak)
        }

        binding.rvSymbols.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvSymbols.adapter = adapter
        adapter.submitList(mockupSymbols)
    }

    private fun speak(text: String) {
        if (text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
        } else {
            Toast.makeText(context, "Error al iniciar TTS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
        _binding = null
    }
}
