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
    private val authRepository: AuthRepository,
    private val userRepository: com.signlink.data.repository.UserRepository,
    private val apiService: com.signlink.data.remote.ApiService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<com.signlink.data.model.User?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        wakeUpServer()
    }

    private fun wakeUpServer() {
        viewModelScope.launch {
            try {
                apiService.wakeUp()
            } catch (e: Exception) {
                // Silently ignore or log
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(email, pass).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    fun loginWithGoogle(idToken: String, isActionLogin: Boolean) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.loginWithGoogle(idToken, isActionLogin).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    private fun handleAuthResult(result: Result<AuthResult>) {
        result.fold(
            onSuccess = { authResult ->
                val uid = authResult.user?.uid
                val email = authResult.user?.email
                if (uid != null) {
                    viewModelScope.launch {
                        userRepository.getUserProfile(uid).onSuccess { existingUser ->
                            if (existingUser == null) {
                                val newUser = com.signlink.data.model.User(
                                    uid = uid,
                                    email = email ?: "",
                                    displayName = authResult.user?.displayName ?: email?.substringBefore("@") ?: "Usuario"
                                )
                                userRepository.saveUserProfile(newUser)
                                _userProfile.value = newUser
                            } else {
                                _userProfile.value = existingUser
                            }
                        }
                    }
                }
                _authState.value = AuthState.Success(authResult)
            },
            onFailure = { _authState.value = AuthState.Error(it.message ?: "Authentication failed") }
        )
    }

    fun checkUserStatus() {
        val user = authRepository.currentUser
        if (user != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(user.uid).onSuccess { profile ->
                    _userProfile.value = profile
                }
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(uid).onSuccess { user ->
                _userProfile.value = user
            }
        }
    }

    fun updateProfileType(profileType: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateProfileType(uid, profileType)
        }
    }

    fun getLocalProfileType() = userRepository.getLocalProfileType()

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(email, pass).collect { result ->
                result.fold(
                    onSuccess = { authResult ->
                        val user = com.signlink.data.model.User(
                            uid = authResult.user?.uid ?: "",
                            email = email,
                            displayName = email.substringBefore("@")
                        )
                        userRepository.saveUserProfile(user)
                        _authState.value = AuthState.Success(authResult)
                    },
                    onFailure = { _authState.value = AuthState.Error(it.message ?: "Registration failed") }
                )
            }
        }
    }

    fun isUserLoggedIn() = authRepository.isUserLoggedIn()

    fun logout() {
        authRepository.logout()
        userRepository.clearLocalData()
        _userProfile.value = null
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val authResult: AuthResult) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
