package com.signlink.ui.alerts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.signlink.R
import com.signlink.databinding.FragmentAlertsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlertsFragment : Fragment(R.layout.fragment_alerts) {
    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlertsBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
