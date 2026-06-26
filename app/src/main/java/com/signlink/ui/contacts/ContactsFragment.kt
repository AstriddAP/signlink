package com.signlink.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import com.signlink.R
import com.signlink.data.model.RecentChat
import com.signlink.data.model.User
import com.signlink.data.repository.UserRepository
import com.signlink.databinding.FragmentContactsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private var contactsListener: ListenerRegistration? = null
    private val activeChatListeners = mutableMapOf<String, List<ListenerRegistration>>()
    private val recentChatsMap = mutableMapOf<String, RecentChat>()

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var contactAdapter: ContactAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentContactsBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        startObservingContacts()
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(
            onContactClick = { contact ->
                val bundle = Bundle().apply {
                    putString("contact_uid", contact.uid)
                    putString("contact_name", contact.displayName)
                    putString("contact_email", contact.email)
                }
                findNavController().navigate(R.id.action_contacts_to_chat, bundle)
            },
            onContactLongClick = { contact ->
                showDeleteContactDialog(contact)
            }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = contactAdapter
    }

    private fun setupListeners() {
        binding.fabAddContact.setOnClickListener {
            findNavController().navigate(R.id.action_contacts_to_qr_scanner)
        }

        binding.fabShowMyQr.setOnClickListener {
            ShowQRDialogFragment.newInstance().show(childFragmentManager, ShowQRDialogFragment.TAG)
        }
    }

    private fun startObservingContacts() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        contactsListener?.remove()
        contactsListener = null
        activeChatListeners.values.forEach { list ->
            list.forEach { it.remove() }
        }
        activeChatListeners.clear()
        recentChatsMap.clear()

        contactsListener = db.collection("users").document(currentUserId)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error al cargar contactos: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val contacts = snapshot.toObjects(User::class.java)
                    
                    if (contacts.isEmpty()) {
                        recentChatsMap.clear()
                        updateContactsListUI()
                        return@addSnapshotListener
                    }

                    // Remover chats de contactos eliminados del mapa local
                    val currentContactUids = contacts.map { it.uid }.toSet()
                    val uidsToRemove = recentChatsMap.keys.filterNot { currentContactUids.contains(it) }
                    uidsToRemove.forEach { uid ->
                        stopChatListenersForContact(uid)
                        recentChatsMap.remove(uid)
                    }

                    contacts.forEach { contact ->
                        val chatId = if (currentUserId < contact.uid) {
                            "${currentUserId}_${contact.uid}"
                        } else {
                            "${contact.uid}_${currentUserId}"
                        }

                        stopChatListenersForContact(contact.uid)
                        val listenersList = mutableListOf<ListenerRegistration>()

                        // 1. Escuchar el último mensaje
                        val lastMsgListener = db.collection("chats").document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { msgSnapshot, msgError ->
                                if (msgError != null) return@addSnapshotListener

                                val lastMsg = if (msgSnapshot != null && !msgSnapshot.isEmpty) {
                                    msgSnapshot.documents.first().toObject(com.signlink.data.model.Message::class.java)
                                } else {
                                    null
                                }

                                val current = recentChatsMap[contact.uid]
                                recentChatsMap[contact.uid] = RecentChat(
                                    contact = contact,
                                    lastMessage = lastMsg,
                                    unreadCount = current?.unreadCount ?: 0
                                )
                                updateContactsListUI()
                            }

                        if (lastMsgListener != null) {
                            listenersList.add(lastMsgListener)
                        }

                        // 2. Escuchar mensajes no leídos
                        val unreadListener = db.collection("chats").document(chatId)
                            .collection("messages")
                            .whereEqualTo("seen", false)
                            .addSnapshotListener { unreadSnapshot, unreadError ->
                                if (unreadError != null) return@addSnapshotListener

                                val unreadCount = if (unreadSnapshot != null) {
                                    unreadSnapshot.documents.count { doc ->
                                        val senderId = doc.getString("senderId") ?: ""
                                        senderId != currentUserId
                                    }
                                } else {
                                    0
                                }

                                val current = recentChatsMap[contact.uid]
                                recentChatsMap[contact.uid] = RecentChat(
                                    contact = contact,
                                    lastMessage = current?.lastMessage,
                                    unreadCount = unreadCount
                                )
                                updateContactsListUI()
                            }

                        if (unreadListener != null) {
                            listenersList.add(unreadListener)
                        }

                        activeChatListeners[contact.uid] = listenersList
                    }
                    updateContactsListUI()
                }
            }
    }

    private fun stopChatListenersForContact(contactUid: String) {
        activeChatListeners[contactUid]?.forEach { it.remove() }
        activeChatListeners.remove(contactUid)
    }

    private fun updateContactsListUI() {
        val binding = _binding ?: return
        
        val sortedList = recentChatsMap.values.toList().sortedWith { c1, c2 ->
            c1.contact.displayName.compareTo(c2.contact.displayName, ignoreCase = true)
        }

        if (sortedList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvContacts.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvContacts.visibility = View.VISIBLE
            contactAdapter.submitList(sortedList)
        }
    }


    private fun showDeleteContactDialog(contact: User) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar contacto")
            .setMessage("¿Estás seguro de que deseas eliminar a ${contact.displayName} de tus contactos?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                deleteContact(contact)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteContact(contact: User) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        lifecycleScope.launch {
            val result = userRepository.deleteContact(currentUser.uid, contact.uid)
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Contacto eliminado: ${contact.displayName}", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(context, "Error al eliminar contacto: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        contactsListener?.remove()
        contactsListener = null
        activeChatListeners.values.forEach { list ->
            list.forEach { it.remove() }
        }
        activeChatListeners.clear()
        super.onDestroyView()
        _binding = null
    }
}

class ContactAdapter(
    private val onContactClick: (User) -> Unit,
    private val onContactLongClick: (User) -> Unit
) : androidx.recyclerview.widget.ListAdapter<RecentChat, ContactAdapter.ContactViewHolder>(RecentChatDiffCallback()) {
 
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }
 
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
 
    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetter = itemView.findViewById<TextView>(R.id.tv_avatar_letter)
        private val tvName = itemView.findViewById<TextView>(R.id.tv_contact_name)
        private val tvLastMsg = itemView.findViewById<TextView>(R.id.tv_contact_last_message)
        private val tvTime = itemView.findViewById<TextView>(R.id.tv_contact_timestamp)
        private val tvUnread = itemView.findViewById<TextView>(R.id.tv_unread_badge)
        private val ivTicks = itemView.findViewById<android.widget.ImageView>(R.id.iv_contact_status_ticks)
 
        fun bind(recentChat: RecentChat) {
            val contact = recentChat.contact
            val lastMsg = recentChat.lastMessage
            val unreadCount = recentChat.unreadCount
 
            tvName.text = contact.displayName
            val initial = if (contact.displayName.isNotEmpty()) contact.displayName.take(1).uppercase() else "U"
            tvLetter.text = initial
 
            if (lastMsg != null) {
                tvLastMsg.text = lastMsg.text
                
                val date = lastMsg.timestamp?.toDate()
                if (date != null) {
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    tvTime.text = sdf.format(date)
                } else {
                    tvTime.text = ""
                }
 
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (lastMsg.senderId == currentUserId) {
                    tvUnread.visibility = View.GONE
                    ivTicks.visibility = View.VISIBLE
                    if (lastMsg.seen) {
                        ivTicks.setImageResource(R.drawable.ic_check_double)
                        ivTicks.setColorFilter(android.graphics.Color.parseColor("#34B7F1"))
                    } else {
                        ivTicks.setImageResource(R.drawable.ic_check)
                        ivTicks.setColorFilter(android.graphics.Color.GRAY)
                    }
                } else {
                    ivTicks.visibility = View.GONE
                    if (unreadCount > 0) {
                        tvUnread.text = unreadCount.toString()
                        tvUnread.visibility = View.VISIBLE
                    } else {
                        tvUnread.visibility = View.GONE
                    }
                }
            } else {
                tvLastMsg.text = contact.email
                tvTime.text = ""
                tvUnread.visibility = View.GONE
                ivTicks.visibility = View.GONE
            }
 
            itemView.setOnClickListener { onContactClick(contact) }
            itemView.setOnLongClickListener {
                onContactLongClick(contact)
                true
            }
        }
    }
 
    class RecentChatDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<RecentChat>() {
        override fun areItemsTheSame(oldItem: RecentChat, newItem: RecentChat): Boolean =
            oldItem.contact.uid == newItem.contact.uid
 
        override fun areContentsTheSame(oldItem: RecentChat, newItem: RecentChat): Boolean =
            oldItem == newItem
    }
}
