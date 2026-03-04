package com.example.bunkr.services

import android.app.assist.AssistStructure
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

        // Identifica os campos baseando-se apenas na ordem visual (Cima para Baixo)
        val (usernameId, passwordId) = findAutofillIds(structure)

        Log.d(TAG, "IDs Detectados -> User: $usernameId | Pass: $passwordId")

        if (usernameId == null && passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@BunkrAutofillService)
                val item = db.bunkrDao().getItemByPackage(packageName)

                if (item != null) {
                    val presentation = RemoteViews(this@BunkrAutofillService.packageName, R.layout.autofill_suggestion)
                    presentation.setTextViewText(R.id.txtSuggestion, "Bunkr: ${item.title}")

                    val datasetBuilder = Dataset.Builder()

                    // O primeiro campo detectado recebe o Usuário
                    usernameId?.let {
                        datasetBuilder.setValue(it, AutofillValue.forText(item.accountName), presentation)
                    }

                    // O segundo campo detectado recebe a Senha
                    passwordId?.let {
                        datasetBuilder.setValue(it, AutofillValue.forText(item.password), presentation)
                    }

                    val response = FillResponse.Builder()
                        .addDataset(datasetBuilder.build())
                        .build()

                    callback.onSuccess(response)
                    Log.d(TAG, "✅ Preenchimento enviado para $packageName")
                } else {
                    callback.onSuccess(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar dados: ${e.message}")
                callback.onSuccess(null)
            }
        }
    }

    private fun findAutofillIds(structure: AssistStructure): Pair<AutofillId?, AutofillId?> {
        val editableFields = mutableListOf<AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            // Filtro ultra simples: Se é visível e é um campo de texto (ou classe EditText)
            val className = node.className ?: ""
            val isEditText = className.contains("EditText") ||
                    node.autofillType == View.AUTOFILL_TYPE_TEXT

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

        // Lógica de posição: 1º campo = User, 2º campo = Pass
        val user = if (editableFields.size >= 1) editableFields[0] else null
        val pass = if (editableFields.size >= 2) editableFields[1] else null

        return Pair(user, pass)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}