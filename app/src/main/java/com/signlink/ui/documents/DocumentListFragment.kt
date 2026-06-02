package com.signlink.ui.documents

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
import com.signlink.data.local.entity.DocumentEntity
import com.signlink.databinding.FragmentDocumentListBinding
import com.signlink.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DocumentListFragment : Fragment(R.layout.fragment_document_list) {

    private var _binding: FragmentDocumentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentViewModel by viewModels()
    private lateinit var adapter: DocumentAdapter

    @Inject
    lateinit var securityManager: SecurityManager

    private var isAuthenticated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDocumentListBinding.bind(view)

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
                title = "Acceso a Documentos",
                onSuccess = {
                    if (isAdded && _binding != null) {
                        isAuthenticated = true
                        observeViewModel()
                    }
                },
                onError = { error ->
                    if (isAdded) {
                        Toast.makeText(context, "Error de acceso: $error", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            )
        } else {
            isAuthenticated = true
            observeViewModel()
        }
    }

    private fun setupRecyclerView() {
        adapter = DocumentAdapter(
            onDocumentClick = { document ->
                if (document.type == "DNI") {
                    val action = DocumentListFragmentDirections.actionNavDocumentsToViewDni(document.id)
                    findNavController().navigate(action)
                }
            },
            onDeleteClick = { document ->
                confirmDelete(document)
            }
        )
        binding.rvDocuments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocuments.adapter = adapter
    }

    private fun confirmDelete(document: DocumentEntity) {
        securityManager.showBiometricPrompt(
            activity = requireActivity(),
            title = "Confirmar eliminación",
            subtitle = "Autentícate para eliminar ${document.title}",
            onSuccess = {
                viewModel.deleteDocument(document)
                Toast.makeText(context, "Documento eliminado", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(context, "No se pudo eliminar: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        binding.fabAddDocument.setOnClickListener {
            findNavController().navigate(R.id.action_nav_documents_to_addDocument)
        }
    }

    private fun observeViewModel() {
        if (_binding == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.documents.collectLatest { documents ->
                _binding?.let { b ->
                    adapter.submitList(documents)
                    b.tvEmptyState.isVisible = documents.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
