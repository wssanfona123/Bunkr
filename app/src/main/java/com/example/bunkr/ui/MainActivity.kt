package com.example.bunkr.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import com.example.bunkr.data.BunkrItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var rvItens: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar

    private val SPREADSHEET_ID = "1wEFYaz5qp6CYmA2Kr2LJBkluZDTHajEQUpwyFGUUziE"

    // 1. Configurar o seletor de conta do Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                iniciarSincronizacao(task.result?.account)
            } else {
                Toast.makeText(this, "Falha no login do Google", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Estética: Layout sem limites para a Toolbar
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(R.layout.activity_main)

        // Inicializar Views
        rvItens = findViewById(R.id.rvItens)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBarSync)

        // Configurar Toolbar e o botão "Hambúrguer"
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configurar RecyclerView
        rvItens.layoutManager = LinearLayoutManager(this)

        // Listener do FAB (Adicionar novo item)
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        // 2. Configurar os Cliques do Menu Lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_sync -> solicitarLoginGoogle()
                R.id.nav_settings -> {
                    // Aqui você chama sua tela de configurações se ela existir
                    Toast.makeText(this, "Configurações em breve", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> realizarLogout()
            }
            drawerLayout.closeDrawers() // Fecha o menu lateral
            true
        }
    }

    private fun solicitarLoginGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        // Forçamos o logout do Google antes para garantir que o usuário escolha a conta
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun iniciarSincronizacao(account: android.accounts.Account?) {
        progressBar.visibility = View.VISIBLE

        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(SheetsScopes.SPREADSHEETS)
        ).setBackOff(ExponentialBackOff()).apply {
            selectedAccount = account
        }

        val service = GoogleSheetsService(this, credential)
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val prefs = getSharedPreferences("sessao_bunkr", MODE_PRIVATE)
                val idLogado = prefs.getInt("usuario_id", -1)

                // Lógica Bidirecional: Compara local vs nuvem
                val itensLocal = db.bunkrDao().getAllItemsByUserSync(idLogado)
                val itensPlanilha = service.importarDaPlanilha(SPREADSHEET_ID, idLogado)

                withContext(Dispatchers.Main) {
                    when {
                        // Nuvem tem mais dados que o local
                        itensPlanilha.size > itensLocal.size -> {
                            sincronizarParaBaixo(db, itensLocal, itensPlanilha)
                        }
                        // Local tem mais dados que a nuvem
                        itensLocal.size > itensPlanilha.size -> {
                            service.exportarParaPlanilha(SPREADSHEET_ID, itensLocal)
                            Toast.makeText(this@MainActivity, "Planilha Atualizada! ⬆️", Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@MainActivity, "Sincronizado! ✅", Toast.LENGTH_SHORT).show()
                    }
                    progressBar.visibility = View.GONE
                    atualizarLista()
                }
            } catch (e: Exception) {
                Log.e("SYNC_ERROR", "Erro: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Erro ao sincronizar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun sincronizarParaBaixo(db: AppDatabase, local: List<BunkrItem>, nuvem: List<BunkrItem>) {
        val novos = nuvem.filter { n ->
            local.none { l -> l.title == n.title && l.accountName == n.accountName }
        }
        novos.forEach { db.bunkrDao().insert(it) }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Baixados ${novos.size} itens! ⬇️", Toast.LENGTH_SHORT).show()
        }
    }

    private fun realizarLogout() {
        val prefs = getSharedPreferences("sessao_bunkr", MODE_PRIVATE)
        prefs.edit().clear().apply()
        // Supondo que sua tela de login se chame "login" ou "LoginActivity"
        val intent = Intent(this, login::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun atualizarLista() {
        val db = AppDatabase.getDatabase(this)
        val idLogado = getSharedPreferences("sessao_bunkr", MODE_PRIVATE).getInt("usuario_id", -1)
        lifecycleScope.launch(Dispatchers.IO) {
            val lista = db.bunkrDao().getItemsByUser(idLogado)
            withContext(Dispatchers.Main) {
                rvItens.adapter = BunkrAdapter(lista)
            }
        }
    }
}