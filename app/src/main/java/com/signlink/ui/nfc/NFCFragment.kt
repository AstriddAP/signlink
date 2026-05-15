package com.signlink.ui.nfc

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R
import com.signlink.databinding.FragmentNfcBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NFCFragment : Fragment(R.layout.fragment_nfc) {
    private var _binding: FragmentNfcBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNfcBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
