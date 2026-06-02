package com.signlink.ui.documents

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.signlink.R
import com.signlink.databinding.FragmentViewDniBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import java.io.File

@AndroidEntryPoint
class ViewDniFragment : Fragment(R.layout.fragment_view_dni) {

    private var _binding: FragmentViewDniBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentViewModel by viewModels()
    private val args: ViewDniFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentViewDniBinding.bind(view)

        setupListeners()
        observeViewModel()
        
        viewModel.getDocumentById(args.documentId)
    }

    private fun setupListeners() {
        // Se eliminó la funcionalidad de audio por requerimiento
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDocument.collectLatest { document ->
                document?.let { doc ->
                    (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = doc.title
                    
                    val images = mutableListOf<String>()
                    doc.frontImagePath?.let { images.add(it) }
                    doc.backImagePath?.let { images.add(it) }
                    
                    val adapter = ImagePagerAdapter(images)
                    binding.viewPager.adapter = adapter
                    
                    TabLayoutMediator(binding.dotsIndicator, binding.viewPager) { _, _ -> }.attach()

                    // Extraer info si es DNI y aún no se ha hecho
                    if (doc.type == "DNI" && viewModel.dniData.value == null && !doc.frontImagePath.isNullOrEmpty()) {
                        val file = File(doc.frontImagePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            viewModel.extractDniInfo(bitmap)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAnalyzing.collectLatest { isAnalyzing ->
                binding.progressBar.isVisible = isAnalyzing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
