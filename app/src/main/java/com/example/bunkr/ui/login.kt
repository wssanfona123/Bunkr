package com.example.bunkr.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class login : AppCompatActivity() {

    private fun setupBiometric() {
        // Verifica se o hardware de biometria está disponível antes de tentar
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometria não disponível neste dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@login, MainActivity::class.java))
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Não dar Toast aqui se o usuário apenas cancelar, senão fica irritante
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Biométrico")
            .setSubtitle("Acesse seu cofre Bunkr")
            .setNegativeButtonText("Usar senha")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Preferências e Banco
        val prefs = getSharedPreferences("config_bunkr", MODE_PRIVATE)
        val estaLiberado = prefs.getBoolean("biometria_ativa", false)
        val db = AppDatabase.Companion.getDatabase(this)
        val dao = db.bunkrDao()

        // 2. Elementos da Interface
        val mainView = findViewById<View>(R.id.main)
        val btnBiometria = findViewById<ImageButton>(R.id.btnBiometria)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val editUser = findViewById<EditText>(R.id.editUser)
        val editPass = findViewById<EditText>(R.id.editPassword)

        // 3. Ajuste de Layout (WindowInsets)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 4. Lógica de visibilidade do ícone de digital
        if (estaLiberado) {
            btnBiometria.visibility = View.VISIBLE
            setupBiometric() // Tenta abrir automático se já logou antes
        } else {
            btnBiometria.visibility = View.GONE
        }

        // 5. Cliques
        btnBiometria.setOnClickListener {
            setupBiometric()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            dao.insertUser(User(username = "admin", password = "123"))
        }

        btnLogin.setOnClickListener {
            val username = editUser.text.toString()
            val password = editPass.text.toString()

            lifecycleScope.launch(Dispatchers.IO) {
                val user = dao.checkLogin(username, password)

                withContext(Dispatchers.Main) {
                    if (user != null) {
                        // Ativa biometria para a PRÓXIMA vez
                        prefs.edit().putBoolean("biometria_ativa", true).apply()

                        startActivity(Intent(this@login, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@login,
                            "Usuário ou senha incorretos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}