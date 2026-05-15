package com.signlink.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.signlink.R
import com.signlink.databinding.FragmentSetupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSetupBinding.bind(view)

        binding.cardSelectAuditivo.setOnClickListener {
            findNavController().navigate(R.id.action_setup_to_home_auditivo)
        }

        binding.cardSelectMudo.setOnClickListener {
            findNavController().navigate(R.id.action_setup_to_home_mudo)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
