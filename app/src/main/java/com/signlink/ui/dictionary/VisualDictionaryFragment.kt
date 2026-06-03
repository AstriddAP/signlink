package com.signlink.ui.dictionary

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R
import com.signlink.databinding.FragmentVisualDictionaryBinding
import dagger.hilt.android.AndroidEntryPoint

import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class VisualDictionaryFragment : Fragment(R.layout.fragment_visual_dictionary) {

    private var _binding: FragmentVisualDictionaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: VisualDictionaryAdapter

    // Lista de ejemplo de LSP (Lengua de Señas Peruana)
    private val fullList = listOf(
        SignLanguageItem(1, "Hola", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJndXN5Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKMGpx6v2M127kc/giphy.gif"),
        SignLanguageItem(2, "Gracias", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJndXN5Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKL9oHWU7z3/giphy.gif"),
        SignLanguageItem(3, "Por favor", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJndXN5Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKDkDbIDJ98H8f6/giphy.gif"),
        SignLanguageItem(4, "Emergencia", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJndXN5Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKVUn7iM8FMEU24/giphy.gif"),
        SignLanguageItem(5, "Amigo", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJndXN5Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6Mnd6JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKL5b7F78UfGvL2/giphy.gif")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVisualDictionaryBinding.bind(view)
        
        setupToolbar()
        setupRecyclerView()
        setupSearch()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = VisualDictionaryAdapter()
        binding.rvDictionary.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        binding.rvDictionary.adapter = adapter
        adapter.submitList(fullList)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = fullList.filter { it.word.lowercase().contains(query) }
                adapter.submitList(filteredList)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
