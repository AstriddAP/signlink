package com.signlink.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signlink.databinding.ItemNewsCardBinding

class NewsAdapter(private val onExplainClick: (NewsItem) -> Unit) :
    ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(private val binding: ItemNewsCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NewsItem) {
            binding.tvTitle.text = item.title
            binding.tvSummary.text = item.explainedContent ?: (item.rawContent.take(150) + "...")
            
            binding.btnReadMore.text = "Simplificar con IA"
            binding.btnReadMore.setOnClickListener {
                onExplainClick(item)
            }

            // Al tocar la tarjeta, mostramos el contenido completo en un diálogo o expandimos
            binding.root.setOnClickListener {
                android.app.AlertDialog.Builder(it.context)
                    .setTitle(item.title)
                    .setMessage(item.rawContent)
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem) = oldItem == newItem
    }
}
