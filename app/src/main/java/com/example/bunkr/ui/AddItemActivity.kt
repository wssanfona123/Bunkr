package com.example.bunkr.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.BunkrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Modelo de dados para a lista de apps
data class InstalledApp(val name: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class AddItemActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        val db = AppDatabase.getDatabase(this)

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnSelectApp = findViewById<Button>(R.id.btnSelectApp) // NOVO BOTÃO
        val editTitle = findViewById<EditText>(R.id.editTitle)
        val editUser = findViewById<EditText>(R.id.editUsername)
        val editPass = findViewById<EditText>(R.id.editPassword)
        val editPackage = findViewById<EditText>(R.id.editPackage)

        // Ação para abrir o seletor de aplicativos
        btnSelectApp.setOnClickListener {
            showAppPicker(editTitle, editPackage)
        }

        btnSave.setOnClickListener {
            val title = editTitle.text.toString()
            val user = editUser.text.toString()
            val pass = editPass.text.toString()
            val pkg = editPackage.text.toString()

            if (title.isNotEmpty() && pass.isNotEmpty()) {
                val newItem = BunkrItem(
                    title = title,
                    accountName = user,
                    password = pass,
                    packageName = if (pkg.isNotEmpty()) pkg else null
                )

                // Esconde o teclado
                val view = this.currentFocus
                if (view != null) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    db.bunkrDao().insert(newItem)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddItemActivity, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Preencha o título e a senha!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função que lista os apps instalados
    private fun getInstalledApps(): List<InstalledApp> {
        val appList = mutableListOf<InstalledApp>()
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            // Pega apenas apps que o usuário pode abrir
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

    // Exibe o diálogo de seleção
    private fun showAppPicker(editTitle: EditText, editPackage: EditText) {
        val apps = getInstalledApps()
        val names = apps.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecione o Aplicativo")
            .setItems(names) { _, which ->
                val selected = apps[which]
                editPackage.setText(selected.packageName)
                // Se o título estiver vazio, preenchemos com o nome do app
                if (editTitle.text.isEmpty()) {
                    editTitle.setText(selected.name)
                }
            }
            .show()
    }
}