package com.signlink.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.signlink.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(email, pass).collect { result ->
                result.fold(
                    onSuccess = { _authState.value = AuthState.Success(it) },
                    onFailure = { _authState.value = AuthState.Error(it.message ?: "Login failed") }
                )
            }
        }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(email, pass).collect { result ->
                result.fold(
                    onSuccess = { _authState.value = AuthState.Success(it) },
                    onFailure = { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
                )
            }
        }
    }

    fun isUserLoggedIn() = authRepository.isUserLoggedIn()

    fun logout() {
        authRepository.logout()
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val authResult: AuthResult) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
