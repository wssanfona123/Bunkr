package com.example.bunkr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bunkr.R
import com.example.bunkr.data.BunkrItem

class BunkrAdapter(private val lista: List<BunkrItem>) :
    RecyclerView.Adapter<BunkrAdapter.BunkrViewHolder>() {

    class BunkrViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTitulo)
        val usuario: TextView = view.findViewById(R.id.txtUsuario)
        val senha: TextView = view.findViewById(R.id.txtSenha)
        val btnCopiar: ImageView = view.findViewById(R.id.btnCopy) // O novo botão
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BunkrViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bunkr, parent, false)
        return BunkrViewHolder(view)
    }

    override fun onBindViewHolder(holder: BunkrViewHolder, position: Int) {
        val item = lista[position]

        holder.titulo.text = item.title
        holder.usuario.text = item.accountName
        holder.senha.text = "••••••••"

        // Lógica para copiar a senha
        holder.btnCopiar.setOnClickListener {
            val context = holder.itemView.context

            // Acessa o serviço de área de transferência do sistema
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            // Cria o pacote de dados (etiqueta + conteúdo)
            val clip = ClipData.newPlainText("Senha Bunkr", item.password)

            // Define o clipe na área de transferência
            clipboard.setPrimaryClip(clip)

            // Feedback visual para o usuário
            Toast.makeText(context, "Senha de ${item.title} copiada!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = lista.size
}