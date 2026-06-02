package com.signlink.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.signlink.R
import com.signlink.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()
    private var isRegisterMode = false

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { viewModel.loginWithGoogle(it, !isRegisterMode) }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Error de Google (10): Revisa tu SHA-1 en Firebase", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        viewModel.resetAuthState()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (isRegisterMode) {
                    viewModel.register(email, pass)
                } else {
                    viewModel.login(email, pass)
                }
            } else {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvRegister.setOnClickListener {
            toggleMode()
        }
    }

    private fun toggleMode() {
        isRegisterMode = !isRegisterMode
        if (isRegisterMode) {
            binding.welcomeTxt.text = getString(R.string.register_title)
            binding.subtitleTxt.text = getString(R.string.register_subtitle)
            binding.btnLogin.text = getString(R.string.register_button)
            binding.tvRegister.text = getString(R.string.already_have_account)
        } else {
            binding.welcomeTxt.text = getString(R.string.welcome_title)
            binding.subtitleTxt.text = getString(R.string.welcome_subtitle)
            binding.btnLogin.text = getString(R.string.login_button)
            binding.tvRegister.text = getString(R.string.no_account)
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        // Forzar a mostrar el selector de cuentas
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.loginProgress.visibility = View.VISIBLE
                        }
                        is AuthViewModel.AuthState.Success -> {
                            binding.loginProgress.visibility = View.GONE
                            Toast.makeText(requireContext(), "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            // Ir directamente al Home unificado
                            findNavController().navigate(R.id.nav_home)
                        }
                        is AuthViewModel.AuthState.Error -> {
                            binding.btnLogin.isEnabled = true
                            binding.loginProgress.visibility = View.GONE
                            val errorMessage = when (state.message) {
                                "USER_NOT_REGISTERED" -> "Esta cuenta no está registrada. Por favor, regístrate primero."
                                else -> "Error: ${state.message}"
                            }
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
