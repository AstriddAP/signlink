package com.signlink.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.signlink.R
import com.signlink.databinding.FragmentNoteListBinding
import com.signlink.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NoteListFragment : Fragment(R.layout.fragment_note_list) {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    @Inject
    lateinit var securityManager: SecurityManager

    private var isAuthenticated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNoteListBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        
        if (!isAuthenticated) {
            authenticate()
        } else {
            observeViewModel()
        }
    }

    private fun authenticate() {
        if (securityManager.isBiometricAvailable(requireContext())) {
            securityManager.showBiometricPrompt(
                activity = requireActivity(),
                onSuccess = {
                    isAuthenticated = true
                    observeViewModel()
                },
                onError = { error ->
                    Toast.makeText(context, "Error de acceso: $error", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            )
        } else {
            // Si no hay biométricos, permitimos el paso por ahora o podrías pedir un PIN
            isAuthenticated = true
            observeViewModel()
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onNoteClick = { note ->
                // Mostrar contenido de la nota
                Toast.makeText(context, "Contenido: ${note.content}", Toast.LENGTH_LONG).show()
            },
            onCopyClick = { note ->
                copyToClipboard(note.content)
            }
        )
        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = adapter
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Nota Segura", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun setupListeners() {
        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notes_to_addNote)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collectLatest { notes ->
                adapter.submitList(notes)
                binding.tvEmptyNotes.isVisible = notes.isEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
