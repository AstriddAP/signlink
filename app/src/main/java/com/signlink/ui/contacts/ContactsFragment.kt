package com.signlink.ui.contacts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R
import com.signlink.databinding.FragmentContactsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactsFragment : Fragment(R.layout.fragment_contacts) {
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
