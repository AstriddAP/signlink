package com.signlink.ui.documents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.signlink.databinding.ItemDocumentImageBinding

class ImagePagerAdapter(private val images: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemDocumentImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(private val binding: ItemDocumentImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath: String) {
            Glide.with(binding.root.context)
                .load(imagePath)
                .into(binding.ivDocumentFull)
        }
    }
}
