package com.signlink.ui.dictionary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signlink.databinding.ItemVisualDictionaryBinding

class VisualDictionaryAdapter : ListAdapter<SignLanguageItem, VisualDictionaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisualDictionaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemVisualDictionaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SignLanguageItem) {
            binding.tvSignWord.text = item.word
            
            Glide.with(binding.root.context)
                .asGif()
                .load(item.gifUrl)
                .centerCrop()
                .into(binding.ivSignGif)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SignLanguageItem>() {
        override fun areItemsTheSame(oldItem: SignLanguageItem, newItem: SignLanguageItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SignLanguageItem, newItem: SignLanguageItem) = oldItem == newItem
    }
}
