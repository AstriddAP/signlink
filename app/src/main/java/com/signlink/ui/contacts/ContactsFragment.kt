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
import com.signlink.R
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
        contactsListener?.remove()
        
        val db = FirebaseFirestore.getInstance()
        contactsListener = db.collection("users").document(currentUser.uid)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error al cargar contactos: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val contacts = snapshot.toObjects(User::class.java)
                    if (contacts.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvContacts.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvContacts.visibility = View.VISIBLE
                        contactAdapter.submitList(contacts)
                    }
                }
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
        super.onDestroyView()
        _binding = null
    }
}

class ContactAdapter(
    private val onContactClick: (User) -> Unit,
    private val onContactLongClick: (User) -> Unit
) : androidx.recyclerview.widget.ListAdapter<User, ContactAdapter.ContactViewHolder>(UserDiffCallback()) {

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
        private val tvEmail = itemView.findViewById<TextView>(R.id.tv_contact_email)

        fun bind(user: User) {
            tvName.text = user.displayName
            tvEmail.text = user.email
            val initial = if (user.displayName.isNotEmpty()) user.displayName.take(1).uppercase() else "U"
            tvLetter.text = initial
            itemView.setOnClickListener { onContactClick(user) }
            itemView.setOnLongClickListener {
                onContactLongClick(user)
                true
            }
        }
    }

    class UserDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
