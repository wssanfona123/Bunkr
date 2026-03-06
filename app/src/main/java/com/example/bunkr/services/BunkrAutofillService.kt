package com.example.bunkr.services

import android.app.assist.AssistStructure
import android.content.Context
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.bunkr.R
import com.example.bunkr.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BunkrAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "BunkrService"

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val packageName = structure.activityComponent.packageName

        // Identifica os campos de entrada na tela do outro app
        val (usernameId, passwordId) = findAutofillIds(structure)

        if (usernameId == null && passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                // 1. Recuperamos o ID do usuário que está atualmente logado no Bunkr
                val prefs = getSharedPreferences("sessao_bunkr", Context.MODE_PRIVATE)
                val idLogado = prefs.getInt("usuario_id", -1)

                // Se não houver ninguém logado, não oferecemos preenchimento por segurança
                if (idLogado == -1) {
                    callback.onSuccess(null)
                    return@launch
                }

                val db = AppDatabase.getDatabase(this@BunkrAutofillService)

                // 2. BUSCA CORRIGIDA: Filtra por Pacote E por Dono (userId)
                // Certifique-se de que essa função existe no seu BunkrDao.kt
                val item = db.bunkrDao().getItemByPackageAndUser(packageName, idLogado)

                if (item != null) {
                    val presentation = RemoteViews(this@BunkrAutofillService.packageName, R.layout.autofill_suggestion)
                    presentation.setTextViewText(R.id.txtSuggestion, "Bunkr: ${item.title}")

                    val datasetBuilder = Dataset.Builder()

                    // Preenche o campo de usuário
                    usernameId?.let {
                        datasetBuilder.setValue(it, AutofillValue.forText(item.accountName), presentation)
                    }

                    // Preenche o campo de senha
                    passwordId?.let {
                        datasetBuilder.setValue(it, AutofillValue.forText(item.password), presentation)
                    }

                    val response = FillResponse.Builder()
                        .addDataset(datasetBuilder.build())
                        .build()

                    callback.onSuccess(response)
                    Log.d(TAG, "✅ Sugestão enviada para $packageName (User ID: $idLogado)")
                } else {
                    callback.onSuccess(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no Autofill: ${e.message}")
                callback.onSuccess(null)
            }
        }
    }

    private fun findAutofillIds(structure: AssistStructure): Pair<AutofillId?, AutofillId?> {
        val editableFields = mutableListOf<AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            val className = node.className ?: ""
            val isEditText = className.contains("EditText") ||
                    node.autofillType == View.AUTOFILL_TYPE_TEXT ||
                    node.hint?.contains("password", ignoreCase = true) == true

            if (node.visibility == View.VISIBLE && isEditText) {
                node.autofillId?.let { editableFields.add(it) }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            traverse(structure.getWindowNodeAt(i).rootViewNode)
        }

        // Lógica simples: Primeiro campo costuma ser user, segundo costuma ser password
        val user = if (editableFields.size >= 1) editableFields[0] else null
        val pass = if (editableFields.size >= 2) editableFields[1] else null

        return Pair(user, pass)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Opcional: Implementar para o Bunkr perguntar se quer salvar novas senhas detectadas
        callback.onSuccess()
    }
}