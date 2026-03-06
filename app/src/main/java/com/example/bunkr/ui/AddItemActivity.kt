package com.example.bunkr.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.BunkrItem
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Modelo de dados para a lista de apps
data class InstalledApp(val name: String, val packageName: String, val icon: Drawable)

class AddItemActivity : AppCompatActivity() {

    private lateinit var imgSelectedIcon: ShapeableImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        val db = AppDatabase.getDatabase(this)

        // Inicializando as Views
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnSelectApp = findViewById<Button>(R.id.btnSelectApp)
        val editTitle = findViewById<EditText>(R.id.editTitle)
        val editUser = findViewById<EditText>(R.id.editUsername)
        val editPass = findViewById<EditText>(R.id.editPassword)
        val editPackage = findViewById<EditText>(R.id.editPackage)
        imgSelectedIcon = findViewById(R.id.imgSelectedIcon)

        btnSelectApp.setOnClickListener {
            showAppPicker(editTitle, editPackage)
        }

        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val title = editTitle.text.toString()
            val user = editUser.text.toString()
            val pass = editPass.text.toString()
            val pkg = editPackage.text.toString()

            // 1. BUSCANDO O ID DO USUÁRIO LOGADO (Correção do erro da linha 66)
            val prefs = getSharedPreferences("sessao_bunkr", Context.MODE_PRIVATE)
            val idLogado = prefs.getInt("usuario_id", -1)

            if (title.isNotEmpty() && pass.isNotEmpty()) {

                // Verificação de segurança: se não houver ID, não salvamos
                if (idLogado == -1) {
                    Toast.makeText(this, "Erro: Usuário não identificado!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newItem = BunkrItem(
                    userId = idLogado, // AGORA PASSANDO O ID CORRETAMENTE
                    title = title,
                    accountName = user,
                    password = pass,
                    packageName = if (pkg.isNotEmpty()) pkg else null
                )

                // Esconde o teclado
                val currentView = this.currentFocus
                if (currentView != null) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentView.windowToken, 0)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 2. CORREÇÃO DO NOME DA FUNÇÃO (Erro da linha 77)
                        // Se o seu DAO usa 'insert', mude aqui para 'insert'.
                        // Se usa 'insertItem', mude aqui para 'insertItem'.
                        db.bunkrDao().insertItem(newItem)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddItemActivity, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddItemActivity, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Preencha o título e a senha!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getInstalledApps(): List<InstalledApp> {
        val appList = mutableListOf<InstalledApp>()
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                appList.add(InstalledApp(
                    app.loadLabel(pm).toString(),
                    app.packageName,
                    app.loadIcon(pm)
                ))
            }
        }
        return appList.sortedBy { it.name }
    }

    private fun showAppPicker(editTitle: EditText, editPackage: EditText) {
        val apps = getInstalledApps()
        val names = apps.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecione o Aplicativo")
            .setItems(names) { _, which ->
                val selected = apps[which]
                editPackage.setText(selected.packageName)
                editTitle.setText(selected.name)
                updateIconUI(selected.icon)
            }
            .show()
    }

    private fun updateIconUI(appIcon: Drawable) {
        imgSelectedIcon.setImageDrawable(appIcon)
        imgSelectedIcon.background = null
        imgSelectedIcon.imageTintList = null
        imgSelectedIcon.setPadding(0, 0, 0, 0)
    }
}