package com.example.bunkr.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val dao: BunkrDao) : ViewModel() {

    // Mudei o nome do parâmetro para 'username' para ficar mais claro
    fun realizarLogin(username: String, pass: String) {
        viewModelScope.launch {
            // 1. Busca o usuário no banco passando o username digitado
            val usuarioBanco = dao.getUserByUsername(username)

            // 2. Verifica se encontrou alguém E se a senha confere
            if (usuarioBanco != null && usuarioBanco.password == pass) {
                // Sucesso!
                // Aqui você deve avisar a sua Activity/Fragment que o login deu certo.
                // Exemplo: Salvar o usuarioBanco.userId no SharedPreferences
            } else {
                // Falha!
                // Usuário não existe ou a senha está errada.
            }
        }
    }
}