package com.example.bunkr.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Este import agora deve funcionar
import kotlinx.coroutines.launch

class LoginViewModel(private val dao: BunkrDao) : ViewModel() {

    fun realizarLogin(user: String, pass: String) {
        viewModelScope.launch {
            val resultado = dao.checkLogin(user, pass)
            if (resultado != null) {
                // Sucesso
            }
        }
    }
}