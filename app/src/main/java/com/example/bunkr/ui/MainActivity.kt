package com.example.bunkr.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Declare o RecyclerView como variável da classe para facilitar o acesso
    private lateinit var rvItens: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        rvItens = findViewById(R.id.rvItens)

        rvItens.layoutManager = LinearLayoutManager(this)

        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                efetuarLogout()
                true
            } else false
        }

        // Configuração correta do clique (sem o clique vazio interno)
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }
    }

    // ESTE METODO É NOVO: Ele roda toda vez que você volta para esta tela
    override fun onResume() {
        super.onResume()
        atualizarLista(rvItens) // Garante que a lista atualiza ao voltar
    }

    private fun atualizarLista(rv: RecyclerView) {
        val db = AppDatabase.Companion.getDatabase(this)
        val dao = db.bunkrDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val listaItens = dao.getAllItems()

            withContext(Dispatchers.Main) {
                // Atualiza o adapter com a lista nova do banco
                rv.adapter = BunkrAdapter(listaItens)

                if (listaItens.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nenhuma conta existente", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun efetuarLogout() {
        val prefs = getSharedPreferences("config_bunkr", MODE_PRIVATE)
        prefs.edit().clear().apply()
        startActivity(Intent(this, login::class.java))
        finish()
    }
}