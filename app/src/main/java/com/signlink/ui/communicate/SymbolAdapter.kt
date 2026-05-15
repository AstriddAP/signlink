package com.signlink.ui.communicate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signlink.R
import com.signlink.data.model.Symbol
import com.signlink.databinding.ItemSymbolBinding

/**
 * Adaptador profesional para el tablero de comunicación AAC.
 * Utiliza ListAdapter para animaciones eficientes y Glide para carga de imágenes.
 */
class SymbolAdapter(
    private val onSymbolClick: (Symbol) -> Unit
) : ListAdapter<Symbol, SymbolAdapter.SymbolViewHolder>(SymbolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val binding = ItemSymbolBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return SymbolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SymbolViewHolder(private val binding: ItemSymbolBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(symbol: Symbol) {
            binding.symbolName.text = symbol.name
            
            // Accesibilidad: El nombre del símbolo es la descripción del contenido
            binding.root.contentDescription = "Símbolo de ${symbol.name}"

            // Carga de imagen con Glide y manejo de errores/placeholders
            if (symbol.imageUrl.isNotEmpty()) {
                Glide.with(binding.symbolImage.context)
                    .load(symbol.imageUrl)
                    .placeholder(R.drawable.ic_chat_bubble)
                    .error(R.drawable.ic_chat_bubble)
                    .into(binding.symbolImage)
            } else {
                // Placeholder predeterminado si no hay URL
                binding.symbolImage.setImageResource(R.drawable.ic_chat_bubble)
            }

            binding.root.setOnClickListener {
                onSymbolClick(symbol)
            }
        }
    }

    /**
     * Callback para calcular las diferencias entre listas de manera eficiente.
     */
    class SymbolDiffCallback : DiffUtil.ItemCallback<Symbol>() {
        override fun areItemsTheSame(oldItem: Symbol, newItem: Symbol): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Symbol, newItem: Symbol): Boolean {
            return oldItem == newItem
        }
    }
}
