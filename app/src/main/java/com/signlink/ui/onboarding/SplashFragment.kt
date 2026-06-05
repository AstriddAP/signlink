package com.signlink.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.signlink.R
import com.signlink.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000) 
            if (findNavController().currentDestination?.id == R.id.splashFragment) {
                if (viewModel.isUserLoggedIn()) {
                    // Ir directamente al Home unificado
                    findNavController().navigate(R.id.action_splash_to_home)
                } else {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
            }
        }
    }
}
