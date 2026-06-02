package com.signlink.ui.home

import androidx.lifecycle.ViewModel
import com.signlink.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeMudoViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    fun getLocalProfileType(): String {
        return userRepository.getLocalProfileType() ?: "habla"
    }
}
