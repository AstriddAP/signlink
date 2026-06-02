package com.signlink.ui.home

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.signlink.data.model.User
import com.signlink.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(uid).onSuccess {
                    _userProfile.value = it
                }.onFailure {
                    // Si falla el remoto, al menos tenemos el local para la UI inmediata
                    _userProfile.value = User(profileType = getLocalProfileType())
                }
            }
        } else {
            _userProfile.value = User(profileType = getLocalProfileType())
        }
    }

    fun getLocalProfileType(): String {
        return userRepository.getLocalProfileType() ?: "auditivo"
    }
}
