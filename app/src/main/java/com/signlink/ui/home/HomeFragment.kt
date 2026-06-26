package com.signlink.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.Priority
import com.signlink.R
import com.signlink.databinding.FragmentHomeBinding
import com.signlink.data.model.RecentChat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    
    private var contactsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val activeChatListeners = mutableMapOf<String, List<com.google.firebase.firestore.ListenerRegistration>>()
    private val recentChatsMap = mutableMapOf<String, RecentChat>()
    private lateinit var recentChatAdapter: RecentChatAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getAndSendLocation()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupUI()
        setupClickListeners()
        setupRecentChatsRecyclerView()
        startObservingRecentChats()
        observeViewModel()
    }

    private fun setupUI() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        binding.tvWelcomeTitle.text = getString(R.string.hello_user, currentUser?.displayName ?: "Usuario")
        
        // Texto fijo para el modo unificado
        binding.tvProfileMode.text = "Panel de Herramientas de Accesibilidad"
    }

    private fun setupClickListeners() {
        // Tarjeta 1: Chat
        binding.cardCommunicate.setOnClickListener {
            findNavController().navigate(R.id.nav_communicate)
        }

        // Cabecera: Emergencia Rápida (Comparte ubicación vía WhatsApp)
        binding.cardEmergency.setOnClickListener {
            sendEmergencyLocation()
        }

        // Tarjeta 2: Servicios Cercanos (abre Mapa de Servicios)
        binding.cardMap.setOnClickListener {
            findNavController().navigate(R.id.nav_map)
        }

        // Tarjeta 3: Modo Escucha
        binding.cardCaptions.setOnClickListener {
            findNavController().navigate(R.id.nav_live_captioning)
        }

        // Tarjeta 4: Transcriptor WhatsApp
        binding.cardAudioTranscriber.setOnClickListener {
            findNavController().navigate(R.id.nav_audio_transcription)
        }

        // Tarjeta 5: IA Explica
        binding.cardAiExplanation.setOnClickListener {
            findNavController().navigate(R.id.nav_ai_explanation)
        }

        // Tarjeta 6: Diccionario de Señas
        binding.cardDictionary.setOnClickListener {
            findNavController().navigate(R.id.nav_dictionary)
        }

        // Botón: Ver Contactos en la sección de Mensajes Recientes
        binding.btnGoToContacts.setOnClickListener {
            findNavController().navigate(R.id.nav_contacts)
        }
    }

    private fun sendEmergencyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getAndSendLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getAndSendLocation() {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                shareLocationText(location)
            } else {
                // Si la última ubicación es nula, solicitamos una actualización en tiempo real
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).setMaxUpdates(1).build()
                
                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val lastLoc = result.lastLocation
                        if (lastLoc != null) {
                            shareLocationText(lastLoc)
                        } else {
                            Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual. Verifica tu GPS.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, android.os.Looper.getMainLooper())
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLocationText(location: Location) {
        val uri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val message = "¡EMERGENCIA! Esta es mi ubicación, tengo algún problema: $uri"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
            val shareIntent = Intent.createChooser(intent, "Enviar ubicación de emergencia")
            startActivity(shareIntent)
        }
    }

    private fun observeViewModel() {
        // No Panic Button to observe
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

    private fun setupRecentChatsRecyclerView() {
        recentChatAdapter = RecentChatAdapter { contact ->
            val bundle = Bundle().apply {
                putString("contact_uid", contact.uid)
                putString("contact_name", contact.displayName)
                putString("contact_email", contact.email)
            }
            findNavController().navigate(R.id.nav_chat, bundle)
        }
        binding.rvRecentChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentChatAdapter
        }
    }

    private fun startObservingRecentChats() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        contactsListener?.remove()
        contactsListener = null
        activeChatListeners.values.forEach { list ->
            list.forEach { it.remove() }
        }
        activeChatListeners.clear()
        recentChatsMap.clear()

        contactsListener = db.collection("users").document(currentUserId)
            .collection("contacts")
            .addSnapshotListener { contactsSnapshot, error ->
                if (error != null) {
                    Log.e("HomeFragment", "Error al escuchar contactos para chats recientes", error)
                    return@addSnapshotListener
                }

                if (contactsSnapshot != null) {
                    val contacts = contactsSnapshot.toObjects(com.signlink.data.model.User::class.java)
                    
                    if (contacts.isEmpty()) {
                        recentChatsMap.clear()
                        updateRecentChatsUI()
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
                        val listenersList = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

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
                                updateRecentChatsUI()
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
                                updateRecentChatsUI()
                            }

                        if (unreadListener != null) {
                            listenersList.add(unreadListener)
                        }

                        activeChatListeners[contact.uid] = listenersList
                    }
                    updateRecentChatsUI()
                }
            }
    }

    private fun stopChatListenersForContact(contactUid: String) {
        activeChatListeners[contactUid]?.forEach { it.remove() }
        activeChatListeners.remove(contactUid)
    }

    private fun updateRecentChatsUI() {
        val binding = _binding ?: return
        
        val sortedList = recentChatsMap.values.toList().sortedWith { c1, c2 ->
            val t1 = c1.lastMessage?.timestamp
            val t2 = c2.lastMessage?.timestamp
            if (t1 != null && t2 != null) {
                t2.compareTo(t1)
            } else if (t1 != null) {
                -1
            } else if (t2 != null) {
                1
            } else {
                c1.contact.displayName.compareTo(c2.contact.displayName)
            }
        }

        if (sortedList.isEmpty()) {
            binding.rvRecentChats.visibility = View.GONE
            binding.tvRecentChatsEmpty.visibility = View.VISIBLE
        } else {
            binding.tvRecentChatsEmpty.visibility = View.GONE
            binding.rvRecentChats.visibility = View.VISIBLE
            recentChatAdapter.submitList(sortedList)
        }
    }
}


class RecentChatAdapter(
    private val onChatClick: (com.signlink.data.model.User) -> Unit
) : androidx.recyclerview.widget.ListAdapter<RecentChat, RecentChatAdapter.RecentViewHolder>(RecentChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_chat, parent, false)
        return RecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLetter = itemView.findViewById<TextView>(R.id.tv_chat_avatar_letter)
        private val tvName = itemView.findViewById<TextView>(R.id.tv_chat_contact_name)
        private val tvLastMsg = itemView.findViewById<TextView>(R.id.tv_chat_last_message)
        private val tvTime = itemView.findViewById<TextView>(R.id.tv_chat_timestamp)
        private val tvUnread = itemView.findViewById<TextView>(R.id.tv_unread_badge)
        private val ivTicks = itemView.findViewById<android.widget.ImageView>(R.id.iv_chat_status_ticks)

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
                tvLastMsg.text = "Inicia una conversación"
                tvTime.text = ""
                tvUnread.visibility = View.GONE
                ivTicks.visibility = View.GONE
            }

            itemView.setOnClickListener { onChatClick(contact) }
        }
    }

    class RecentChatDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<RecentChat>() {
        override fun areItemsTheSame(oldItem: RecentChat, newItem: RecentChat): Boolean =
            oldItem.contact.uid == newItem.contact.uid

        override fun areContentsTheSame(oldItem: RecentChat, newItem: RecentChat): Boolean =
            oldItem == newItem
    }
}
