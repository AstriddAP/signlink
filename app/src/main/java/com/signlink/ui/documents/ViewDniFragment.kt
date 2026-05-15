package com.signlink.ui.documents

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

@AndroidEntryPoint
class ViewDniFragment : Fragment(R.layout.fragment_view_dni) {

    private var _binding: FragmentViewDniBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentViewModel by viewModels()
    private val args: ViewDniFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentViewDniBinding.bind(view)

        observeViewModel()
        
        viewModel.getDocumentById(args.documentId)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDocument.collectLatest { document ->
                document?.let { doc ->
                    // Actualizar el título en la Toolbar global de la Activity
                    (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = doc.title
                    
                    val images = mutableListOf<String>()
                    doc.frontImagePath?.let { images.add(it) }
                    doc.backImagePath?.let { images.add(it) }
                    
                    val adapter = ImagePagerAdapter(images)
                    binding.viewPager.adapter = adapter
                    
                    TabLayoutMediator(binding.dotsIndicator, binding.viewPager) { _, _ -> }.attach()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
