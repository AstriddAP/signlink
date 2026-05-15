package com.signlink.ui.captioning

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signlink.databinding.ItemCaptionBinding

class CaptionAdapter : ListAdapter<LiveCaptioningViewModel.Caption, CaptionAdapter.CaptionViewHolder>(DiffCallback) {

    class CaptionViewHolder(private val binding: ItemCaptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(caption: LiveCaptioningViewModel.Caption) {
            binding.tvCaptionText.text = caption.text
            binding.tvSpeaker.text = caption.speaker
            binding.tvSpeaker.setTextColor(Color.parseColor(caption.color))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaptionViewHolder {
        return CaptionViewHolder(
            ItemCaptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CaptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<LiveCaptioningViewModel.Caption>() {
        override fun areItemsTheSame(oldItem: LiveCaptioningViewModel.Caption, newItem: LiveCaptioningViewModel.Caption): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: LiveCaptioningViewModel.Caption, newItem: LiveCaptioningViewModel.Caption): Boolean {
            return oldItem.text == newItem.text
        }
    }
}
