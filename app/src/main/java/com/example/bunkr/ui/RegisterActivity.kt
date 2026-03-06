package com.example.bunkr.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.User
import com.example.bunkr.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private var fotoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val dao = AppDatabase.getDatabase(this).bunkrDao()

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizeRegister)
        val editNome = findViewById<EditText>(R.id.regNome)
        val editUser = findViewById<EditText>(R.id.regUser)
        val editPass = findViewById<EditText>(R.id.regPass)

        btnFinalizar.setOnClickListener {
            // Trim remove espaços e Lowercase evita problemas de 'Admin' vs 'admin'
            val nome = editNome.text.toString().trim()
            val user = editUser.text.toString().trim().lowercase()
            val pass = editPass.text.toString().trim()

            if (nome.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val securePass = SecurityUtils.encrypt(pass)

                        // Dentro do lifecycleScope da RegisterActivity:
                        val newUser = User(
                            nome = nome,
                            username = user,
                            password = securePass,
                            passwordHint = "Dica para $nome"
                            // Os campos id, dataCriacao, status e dispositivo já têm valores padrão!
                        )
                        dao.insertUser(newUser)

                        // Log para confirmar que o Room realmente processou
                        Log.d("BUNKR_DB", "Usuário cadastrado com sucesso: $user")

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("BUNKR_DB", "Erro ao salvar usuário: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Erro ao salvar no banco!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}