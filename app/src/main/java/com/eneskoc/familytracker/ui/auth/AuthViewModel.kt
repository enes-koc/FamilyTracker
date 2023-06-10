package com.eneskoc.familytracker.ui.auth

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eneskoc.familytracker.data.AuthRepository
import com.eneskoc.familytracker.data.Resource
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginFlow = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val loginFlow: StateFlow<Resource<FirebaseUser>?> = _loginFlow

    private val _signupFlow = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val signupFlow: StateFlow<Resource<FirebaseUser>?> = _signupFlow

    private val _sendDataFlow = MutableStateFlow<Resource<Unit>?>(null)
    val sendDataFlow: StateFlow<Resource<Unit>?> = _sendDataFlow

    private val _findUserFlow = MutableStateFlow<Resource<UserDataHolder>?>(null)
    val findUserFlow: StateFlow<Resource<UserDataHolder>?> = _findUserFlow

    val currentUser: FirebaseUser?
        get() = repository.currentUser
    
    init {
        if(repository.currentUser!=null){
            _loginFlow.value= Resource.Success(repository.currentUser!!)
        }
    }

    fun findUser(username:String) = viewModelScope.launch {
        _findUserFlow.value = Resource.Loading
        val result = repository.findUser(username)
        _findUserFlow.value = result
    }

    fun sendLocationData(location: Location, batteryLevel:Int) = viewModelScope.launch {
        _sendDataFlow.value = Resource.Loading
        val result = repository.sendLocationData(location, batteryLevel)
        _sendDataFlow.value = result
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _loginFlow.value = Resource.Loading
        val result = repository.login(email, password)
        _loginFlow.value = result
    }

    fun signup(name: String,username:String, email: String, password: String) = viewModelScope.launch {
        _signupFlow.value = Resource.Loading
        val result = repository.signup(name, username,email, password)
        _signupFlow.value = result
    }

    fun logout() {
        repository.logout()
        _loginFlow.value = null
        _signupFlow.value = null
    }

}