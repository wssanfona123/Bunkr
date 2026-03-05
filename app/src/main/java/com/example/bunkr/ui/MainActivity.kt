package com.example.bunkr.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
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

    private lateinit var rvItens: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém o visual Full Screen que configuramos
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_main)

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        rvItens = findViewById(R.id.rvItens)

        rvItens.layoutManager = LinearLayoutManager(this)

        // O Logout e o Menu da Toolbar foram removidos daqui

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarLista(rvItens)
    }

    private fun atualizarLista(rv: RecyclerView) {
        val db = AppDatabase.getDatabase(this)
        val dao = db.bunkrDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val listaItens = dao.getAllItems()

            withContext(Dispatchers.Main) {
                rv.adapter = BunkrAdapter(listaItens)

                if (listaItens.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nenhuma conta existente", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    // Função efetuarLogout removida com sucesso
}