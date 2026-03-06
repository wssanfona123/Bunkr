package com.example.bunkr.ui

import android.content.Context // Importação necessária para SharedPreferences
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.BunkrDao
import com.example.bunkr.data.User
import com.example.bunkr.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class login : AppCompatActivity() {

    private lateinit var editUser: EditText
    private lateinit var editPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnBiometria: ImageButton
    private lateinit var dao: BunkrDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val db = AppDatabase.getDatabase(this)
        dao = db.bunkrDao()

        editUser = findViewById(R.id.editUser)
        editPass = findViewById(R.id.editPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnBiometria = findViewById(R.id.btnBiometria)
        val txtCriarConta = findViewById<TextView>(R.id.txtCriarConta)

        val prefs = getSharedPreferences("config_bunkr", MODE_PRIVATE)
        val biometriaAtivaNoApp = prefs.getBoolean("biometria_ativa", false)

        txtCriarConta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        if (biometriaAtivaNoApp && isBiometricAvailable()) {
            btnBiometria.visibility = View.VISIBLE
        } else {
            btnBiometria.visibility = View.GONE
        }

        btnBiometria.setOnClickListener { setupBiometric() }
        btnLogin.setOnClickListener { executarLogin() }
    }

    private fun executarLogin() {
        val userTxt = editUser.text.toString().trim().lowercase()
        val passTxt = editPass.text.toString().trim()

        if (userTxt.isEmpty() || passTxt.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val user = dao.getUserByUsername(userTxt)

            withContext(Dispatchers.Main) {
                if (user == null) {
                    Toast.makeText(this@login, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val agora = System.currentTimeMillis()

                if (user.status != "ativo") {
                    Toast.makeText(this@login, "Esta conta está ${user.status}", Toast.LENGTH_LONG).show()
                    return@withContext
                }

                if (user.lockedUntil != null && agora < user.lockedUntil!!) {
                    val restam = (user.lockedUntil!! - agora) / 1000
                    Toast.makeText(this@login, "Bloqueado! Tente em $restam s.", Toast.LENGTH_LONG).show()
                    aplicarBloqueioVisual(true)
                    return@withContext
                }

                val senhaDecifrada = try {
                    SecurityUtils.decrypt(user.password).trim()
                } catch (e: Exception) {
                    Log.e("BUNKR_AUTH", "Erro ao descriptografar: ${e.message}")
                    null
                }

                if (senhaDecifrada != null && senhaDecifrada == passTxt) {
                    // SUCESSO: ATUALIZAR USUÁRIO NO BANCO
                    user.loginAttempts = 0
                    user.lockedUntil = null
                    user.lastLogin = agora
                    lifecycleScope.launch(Dispatchers.IO) { dao.insertUser(user) }

                    // --- PASSO CRUCIAL: SALVAR O ID DO USUÁRIO LOGADO ---
                    val sessaoPrefs = getSharedPreferences("sessao_bunkr", Context.MODE_PRIVATE)
                    sessaoPrefs.edit().putInt("usuario_id", user.userId).apply() // Mude para .userId
                    val configPrefs = getSharedPreferences("config_bunkr", MODE_PRIVATE)
                    if (isBiometricAvailable()) configPrefs.edit().putBoolean("biometria_ativa", true).apply()

                    irParaMain()
                } else {
                    // FALHA: INCREMENTAR TENTATIVAS
                    user.loginAttempts += 1
                    if (user.loginAttempts >= 5) {
                        user.lockedUntil = agora + (60 * 1000)
                        aplicarBloqueioVisual(true)
                    } else {
                        if (user.loginAttempts >= 3) exibirDicaDeSenha(user)
                    }
                    lifecycleScope.launch(Dispatchers.IO) { dao.insertUser(user) }
                    editPass.text.clear()
                    Toast.makeText(this@login, "Senha incorreta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun aplicarBloqueioVisual(bloquear: Boolean) {
        editUser.isEnabled = !bloquear
        editPass.isEnabled = !bloquear
        btnLogin.isEnabled = !bloquear
        btnLogin.alpha = if (bloquear) 0.5f else 1.0f
    }

    private fun exibirDicaDeSenha(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Dica de Segurança")
            .setMessage("Lembrete: ${user.passwordHint ?: "Nenhuma dica cadastrada"}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun setupBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    // Nota: Para biometria funcionar corretamente com múltiplos usuários,
                    // idealmente você deve ter salvo o ID do último usuário logado
                    // com sucesso para saber de quem carregar as senhas agora.
                    irParaMain()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Cofre Bunkr")
            .setNegativeButtonText("Usar Senha")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun irParaMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}