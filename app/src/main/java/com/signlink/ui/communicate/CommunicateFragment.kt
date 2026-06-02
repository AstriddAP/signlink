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
    
    private lateinit var allPhrases: List<Symbol>
    private lateinit var symbolAdapter: SymbolAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCommunicateBinding.bind(view)
        
        tts = TextToSpeech(requireContext(), this)
        setupPhrasesData()
        setupSymbolsGrid()
        setupCategoryListeners()

        binding.btnSpeak.setOnClickListener {
            val text = binding.etMessage.text.toString()
            speak(text)
        }
    }

    private fun setupPhrasesData() {
        allPhrases = listOf(
            // --- SOCIAL (Cortesía y Saludos) ---
            Symbol("1", "Hola", "Social", "", "Hola, ¿cómo estás?"),
            Symbol("2", "Gracias", "Social", "", "Muchas gracias por tu ayuda"),
            Symbol("3", "De nada", "Social", "", "De nada, un placer"),
            Symbol("4", "Adiós", "Social", "", "Hasta luego, que tenga un buen día"),
            Symbol("5", "Lo siento", "Social", "", "Lo siento mucho, disculpe la molestia"),
            Symbol("21", "Mucho gusto", "Social", "", "Mucho gusto en conocerte"),
            Symbol("22", "Buenos días", "Social", "", "Buenos días"),
            Symbol("23", "Buenas tardes", "Social", "", "Buenas tardes"),
            Symbol("24", "Buenas noches", "Social", "", "Buenas noches, que descanses"),
            Symbol("25", "¿Nombre?", "Social", "", "¿Cómo te llamas?"),
            Symbol("26", "Cuídate", "Social", "", "Cuídate mucho, nos vemos pronto"),
            Symbol("50", "Bienvenido", "Social", "", "Bienvenido, pasa adelante"),
            Symbol("51", "Felicidades", "Social", "", "¡Muchas felicidades!"),
            Symbol("52", "Igualmente", "Social", "", "Muchas gracias, igualmente para ti"),
            Symbol("53", "No importa", "Social", "", "No te preocupes, no tiene importancia"),

            // --- VIDA COTIDIANA (Necesidades y Hogar) ---
            Symbol("6", "Hambre", "Vida Cotidiana", "", "Ya tengo hambre, ¿podemos comer algo?"),
            Symbol("7", "Sed", "Vida Cotidiana", "", "Tengo mucha sed, ¿tienes un poco de agua?"),
            Symbol("8", "Baño", "Vida Cotidiana", "", "¿Me podrías decir dónde está el baño?"),
            Symbol("9", "Cansado", "Vida Cotidiana", "", "Estoy un poco cansado, necesito sentarme un momento"),
            Symbol("10", "Dormir", "Vida Cotidiana", "", "Tengo sueño, ya quiero ir a dormir"),
            Symbol("27", "Frío", "Vida Cotidiana", "", "Tengo frío, ¿podrías cerrar la ventana?"),
            Symbol("28", "Calor", "Vida Cotidiana", "", "Hace mucho calor aquí, ¿podemos prender el ventilador?"),
            Symbol("29", "No entiendo", "Vida Cotidiana", "", "No entiendo bien, ¿puedes escribirlo o usar señas?"),
            Symbol("30", "Batería", "Vida Cotidiana", "", "Mi celular se quedó sin batería, ¿tienes un cargador?"),
            Symbol("31", "Dinero", "Vida Cotidiana", "", "Necesito ir al cajero para sacar un poco de dinero"),
            Symbol("32", "Comprar", "Vida Cotidiana", "", "Quiero comprar esto, ¿me ayudas con el precio?"),
            Symbol("54", "Llaves", "Vida Cotidiana", "", "No encuentro mis llaves, ¿las has visto?"),
            Symbol("55", "Limpiar", "Vida Cotidiana", "", "Esto está sucio, hay que limpiarlo"),
            Symbol("56", "Ropa", "Vida Cotidiana", "", "Necesito cambiarme de ropa"),
            Symbol("57", "Internet", "Vida Cotidiana", "", "No tengo señal de internet aquí"),

            // --- PERMISOS (Respeto y Espacio) ---
            Symbol("11", "Pasar", "Permisos", "", "Con permiso, ¿puedo pasar?"),
            Symbol("12", "Salir", "Permisos", "", "¿Me das permiso para salir un momento?"),
            Symbol("13", "Sentarme", "Permisos", "", "¿Está ocupado este asiento? ¿Me puedo sentar?"),
            Symbol("14", "Hablar", "Permisos", "", "¿Puedo decir algo? Necesito tu atención un segundo"),
            Symbol("33", "Ventana", "Permisos", "", "¿Me permites abrir la ventana?"),
            Symbol("34", "Luz", "Permisos", "", "¿Puedo prender la luz? No veo muy bien"),
            Symbol("35", "Teléfono", "Permisos", "", "¿Me prestas tu teléfono para una emergencia?"),
            Symbol("36", "Un momento", "Permisos", "", "Espérame un momento, por favor"),
            Symbol("58", "Entrar", "Permisos", "", "¿Se puede entrar?"),
            Symbol("59", "Tocar", "Permisos", "", "¿Puedo ver eso? ¿lo puedo tocar?"),
            Symbol("60", "Cerrar", "Permisos", "", "¿Te importa si cierro la puerta?"),

            // --- SALUD (Bienestar y Emergencias) ---
            Symbol("15", "Me duele", "Salud", "", "Me duele aquí, me siento mal"),
            Symbol("16", "Medicina", "Salud", "", "Es hora de tomar mi medicina, ¿me puedes alcanzar agua?"),
            Symbol("17", "Mareo", "Salud", "", "Me siento mareado, por favor ayúdame a sentarme"),
            Symbol("18", "Emergencia", "Salud", "", "¡Es una emergencia! llame a una ambulancia rápido"),
            Symbol("37", "Doctor", "Salud", "", "Necesito ver a un médico pronto"),
            Symbol("38", "Alergia", "Salud", "", "Soy alérgico a este componente, tenga cuidado"),
            Symbol("39", "Fiebre", "Salud", "", "Creo que tengo fiebre, mi cabeza está muy caliente"),
            Symbol("40", "Aire", "Salud", "", "Me falta el aire, no puedo respirar bien"),
            Symbol("61", "Estómago", "Salud", "", "Me duele mucho el estómago, creo que algo me cayó mal"),
            Symbol("62", "Cabeza", "Salud", "", "Tengo un fuerte dolor de cabeza, necesito silencio"),
            Symbol("63", "Ayuda", "Salud", "", "Por favor, ayúdeme, no me siento nada bien"),
            Symbol("64", "Lentes", "Salud", "", "No veo bien, ¿dónde dejé mis lentes?"),

            // --- PREGUNTAS (Información y Rutas) ---
            Symbol("19", "¿Cómo?", "Preguntas", "", "¿Cómo se hace esto? ¿me podrías enseñar?"),
            Symbol("20", "¿Dónde?", "Preguntas", "", "¿Dónde estamos ahora? Creo que me perdí"),
            Symbol("41", "¿Qué hora?", "Preguntas", "", "¿Disculpa, qué hora tienes en tu reloj?"),
            Symbol("42", "¿Precio?", "Preguntas", "", "¿Cuánto cuesta esto? ¿está en oferta?"),
            Symbol("43", "¿Wifi?", "Preguntas", "", "¿Cuál es la contraseña del wifi de este lugar?"),
            Symbol("44", "¿Bus?", "Preguntas", "", "¿Qué autobús o bus debo tomar para ir al centro?"),
            Symbol("45", "¿Qué pasó?", "Preguntas", "", "¿Qué está pasando? no pude escuchar lo que dijeron"),
            Symbol("46", "¿Lejos?", "Preguntas", "", "¿Está muy lejos de aquí o puedo ir caminando?"),
            Symbol("65", "¿Quién?", "Preguntas", "", "¿Quién es esa persona que me está hablando?"),
            Symbol("66", "¿Cuándo?", "Preguntas", "", "¿A qué hora empieza la reunión?"),
            Symbol("67", "¿Por qué?", "Preguntas", "", "No entiendo la razón, ¿por qué pasa esto?"),
            Symbol("68", "¿Ayuda?", "Preguntas", "", "¿Me podrías ayudar con estas bolsas?")
        )
    }

    private fun setupSymbolsGrid() {
        symbolAdapter = SymbolAdapter { symbol ->
            speak(symbol.textToSpeak)
            binding.etMessage.setText(symbol.textToSpeak)
            binding.etMessage.setSelection(binding.etMessage.text.length)
        }

        binding.rvSymbols.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSymbols.adapter = symbolAdapter
        
        // Mostrar todas por defecto
        symbolAdapter.submitList(allPhrases)
    }

    private fun setupCategoryListeners() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull() ?: R.id.chip_all
            
            val filteredList = when (selectedId) {
                R.id.chip_social -> allPhrases.filter { it.category == "Social" }
                R.id.chip_needs -> allPhrases.filter { it.category == "Vida Cotidiana" }
                R.id.chip_permiso -> allPhrases.filter { it.category == "Permisos" }
                R.id.chip_health -> allPhrases.filter { it.category == "Salud" }
                R.id.chip_questions -> allPhrases.filter { it.category == "Preguntas" }
                else -> allPhrases
            }
            
            symbolAdapter.submitList(filteredList)
        }
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
