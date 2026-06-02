package com.signlink.ui.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signlink.R
import com.signlink.data.local.entity.DocumentEntity
import com.signlink.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val onDocumentClick: (DocumentEntity) -> Unit,
    private val onDeleteClick: (DocumentEntity) -> Unit
) : ListAdapter<DocumentEntity, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(document: DocumentEntity) {
            binding.tvDocumentTitle.text = document.title
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvDocumentDate.text = "Añadido el ${sdf.format(Date(document.createdAt))}"

            // Set icon based on type
            val iconRes = when (document.type) {
                "DNI" -> android.R.drawable.ic_menu_agenda
                "CONADIS" -> android.R.drawable.ic_menu_view
                "CV" -> android.R.drawable.ic_menu_save
                else -> android.R.drawable.ic_menu_view
            }
            binding.ivDocumentType.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onDocumentClick(document)
            }

            binding.btnDeleteDocument.setOnClickListener {
                onDeleteClick(document)
            }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentEntity>() {
        override fun areItemsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem == newItem
        }
    }
}
