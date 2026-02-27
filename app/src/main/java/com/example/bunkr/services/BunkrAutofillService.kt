package com.example.bunkr.services

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveInfo
import android.text.InputType
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
import java.util.regex.Pattern

class BunkrAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "BunkrService"

    private fun findAutofillIds(structure: AssistStructure): Pair<AutofillId?, AutofillId?> {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null

        fun traverseNode(node: AssistStructure.ViewNode) {
            val className = node.className ?: ""
            val hint = node.hint?.toString()?.lowercase() ?: ""
            val viewId = node.idEntry?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

            // Verifica se é um campo de entrada de texto
            val isProbablyField = className.contains("EditText") ||
                    className.contains("TextField") ||
                    node.autofillType == View.AUTOFILL_TYPE_TEXT

            if (isProbablyField) {
                // Lógica para Usuário
                if (hint.contains("email") || hint.contains("usuário") || hint.contains("user") ||
                    hint.contains("nome") || viewId.contains("login") || viewId.contains("user") ||
                    contentDesc.contains("usuário")
                ) {
                    if (usernameId == null) usernameId = node.autofillId
                }

                // Lógica para Senha
                val isPasswordType = (node.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                        (node.inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0

                if (hint.contains("senha") || hint.contains("password") ||
                    viewId.contains("password") || viewId.contains("senha") || isPasswordType
                ) {
                    if (passwordId == null) passwordId = node.autofillId
                }
            }

            for (i in 0 until node.childCount) {
                traverseNode(node.getChildAt(i))
            }
        }

        for (i in 0 until structure.windowNodeCount) {
            traverseNode(structure.getWindowNodeAt(i).rootViewNode)
        }
        return Pair(usernameId, passwordId)
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val packageName = structure.activityComponent.packageName
        val (usernameId, passwordId) = findAutofillIds(structure)

        if (usernameId == null && passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            val db = AppDatabase.getDatabase(this@BunkrAutofillService)
            val item = db.bunkrDao().getItemByPackage(packageName)

            if (item != null) {
                val presentation = RemoteViews(this@BunkrAutofillService.packageName, R.layout.autofill_suggestion)
                presentation.setTextViewText(R.id.txtSuggestion, "Bunkr: ${item.title}")

                val datasetBuilder = Dataset.Builder()

                // O filtro ".*" garante que apareça mesmo que o campo já tenha texto
                val filter = Pattern.compile(".*", Pattern.CASE_INSENSITIVE)

                if (usernameId != null) {
                    datasetBuilder.setValue(usernameId, AutofillValue.forText(item.accountName), filter, presentation)
                }

                if (passwordId != null) {
                    // Se não houver campo de usuário, a senha vira a âncora do balão
                    if (usernameId == null) {
                        datasetBuilder.setValue(passwordId, AutofillValue.forText(item.password), filter, presentation)
                    } else {
                        datasetBuilder.setValue(passwordId, AutofillValue.forText(item.password))
                    }
                }

                val response = FillResponse.Builder()
                    .addDataset(datasetBuilder.build())
                    .build()

                callback.onSuccess(response)
                Log.d(TAG, "Dataset enviado para $packageName")
            } else {
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}