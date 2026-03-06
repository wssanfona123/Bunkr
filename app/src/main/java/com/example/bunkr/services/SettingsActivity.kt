import android.content.Intent
import android.util.Log
import com.example.bunkr.data.AppDatabase // Endereço do seu Banco de Dados
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bunkr.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import java.util.Collections


class MainActivity : AppCompatActivity() {

    // ID da sua planilha (pegue da URL do navegador)
    private val SPREADSHEET_ID = "1wEFYaz5qp6CYmA2Kr2LJBkluZDTHajEQUpwyFGUUziE"

    // 1. Configurar o seletor de conta do Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.result

                // Se o login deu certo, iniciamos a sincronização
                iniciarSincronizacao(account.account)
            } else {
                Toast.makeText(this, "Falha no login do Google", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... seu código de inicialização ...
    }
    private fun solicitarLoginGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS)) // Permissão para o Sheets
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun iniciarSincronizacao(account: android.accounts.Account?) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(SheetsScopes.SPREADSHEETS)
        ).setBackOff(ExponentialBackOff())

        credential.selectedAccount = account

        val service = GoogleSheetsService(this, credential)
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Pegamos o ID do usuário logado no Bunkr
                val prefs = getSharedPreferences("sessao_bunkr", MODE_PRIVATE)
                val idLogado = prefs.getInt("usuario_id", -1)

                // Buscamos os itens do banco de dados
                val itens = db.bunkrDao().getAllItemsByUserSync(idLogado)

                // Enviamos para o Google Sheets
                service.exportarParaPlanilha(SPREADSHEET_ID, itens)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Sincronizado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("SYNC_ERROR", "Erro: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao sincronizar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}