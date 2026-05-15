package com.signlink.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.signlink.R
import com.signlink.data.local.entity.NoteEntity
import com.signlink.databinding.ItemNoteBinding

class NoteAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onCopyClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvNoteTitle.text = note.title
            
            val iconRes = when (note.type) {
                "PASSWORD" -> android.R.drawable.ic_lock_idle_lock
                "EMAIL" -> android.R.drawable.ic_dialog_email
                "PIN" -> android.R.drawable.ic_lock_lock
                else -> android.R.drawable.ic_menu_edit
            }
            binding.ivNoteType.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onNoteClick(note)
            }

            binding.ivCopy.setOnClickListener {
                onCopyClick(note)
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}
