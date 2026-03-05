package com.example.bunkr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bunkr.R
import com.example.bunkr.data.BunkrItem

class BunkrAdapter(private var lista: List<BunkrItem>) :
    RecyclerView.Adapter<BunkrAdapter.BunkrViewHolder>() {

    private var listaCompleta: List<BunkrItem> = lista

    class BunkrViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.txtTitulo)
        val usuario: TextView = view.findViewById(R.id.txtUsuario)
        val icone: ImageView = view.findViewById(R.id.imgAppIcon)
        val btnCopiar: ImageButton = view.findViewById(R.id.btnCopy)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BunkrViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bunkr, parent, false)
        return BunkrViewHolder(view)
    }

    override fun onBindViewHolder(holder: BunkrViewHolder, position: Int) {
        val item = lista[position]
        val context = holder.itemView.context
        val pm = context.packageManager

        holder.titulo.text = item.title
        holder.usuario.text = item.accountName

        // --- LÓGICA DE BUSCA DE ÍCONE REAL NO SISTEMA ---
        try {
            val installedApps = pm.getInstalledApplications(0)
            // Busca aproximada: ex: se o título é "Face", encontra "Facebook"
            val appInfo = installedApps.find {
                val label = it.loadLabel(pm).toString().lowercase()
                val busca = item.title.lowercase()
                label.contains(busca) || busca.contains(label)
            }

            if (appInfo != null) {
                holder.icone.setImageDrawable(appInfo.loadIcon(pm))
                holder.icone.background = null        // Remove o gradiente roxo
                holder.icone.imageTintList = null    // Permite as cores originais do app
                holder.icone.setPadding(0, 0, 0, 0)  // Remove o espaço interno
            } else {
                setDefaultIcon(holder)
            }
        } catch (e: Exception) {
            setDefaultIcon(holder)
        }

        // Lógica para Copiar Senha
        holder.btnCopiar.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Senha Bunkr", item.password)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Senha de ${item.title} copiada!", Toast.LENGTH_SHORT).show()
        }

        // Lógica para Editar Item
        holder.btnEditar.setOnClickListener {
            val intent = Intent(context, AddItemActivity::class.java)
            intent.putExtra("ITEM_ID", item.id)
            context.startActivity(intent)
        }
    }

    private fun setDefaultIcon(holder: BunkrViewHolder) {
        holder.icone.setImageResource(R.drawable.email_1_svgrepo_com)
        holder.icone.setBackgroundResource(R.drawable.bg_button_gradient)
        holder.icone.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        val p = (8 * holder.itemView.resources.displayMetrics.density).toInt()
        holder.icone.setPadding(p, p, p, p)
    }

    override fun getItemCount() = lista.size

    fun filter(query: String) {
        lista = if (query.isEmpty()) {
            listaCompleta
        } else {
            listaCompleta.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.accountName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}