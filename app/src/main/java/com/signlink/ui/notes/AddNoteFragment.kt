package com.signlink.ui.notes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.signlink.R
import com.signlink.data.local.entity.NoteEntity
import com.signlink.databinding.FragmentAddNoteBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddNoteFragment : Fragment(R.layout.fragment_add_note) {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddNoteBinding.bind(view)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSaveNote.setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {
        val title = binding.etNoteTitle.text.toString()
        val content = binding.etNoteContent.text.toString()
        val type = when (binding.toggleGroupNoteType.checkedButtonId) {
            R.id.btn_type_password -> "PASSWORD"
            R.id.btn_type_email -> "EMAIL"
            R.id.btn_type_pin -> "PIN"
            else -> "GENERAL"
        }

        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(context, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val note = NoteEntity(
            title = title,
            content = content,
            type = type
        )

        viewModel.addNote(note)
        Toast.makeText(context, "Nota guardada con éxito", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
