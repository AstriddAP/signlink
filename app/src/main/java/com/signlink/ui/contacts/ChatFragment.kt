package com.signlink.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.signlink.R
import com.signlink.data.model.Message
import com.signlink.databinding.FragmentChatBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.signlink.data.remote.FcmSender
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var messageAdapter: MessageAdapter
    private var chatId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var contactUid: String = ""
    private var contactName: String = ""
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        // Obtener argumentos
        contactUid = arguments?.getString("contact_uid") ?: ""
        contactName = arguments?.getString("contact_name") ?: "Contacto"

        // Configurar título del ActionBar
        (activity as? AppCompatActivity)?.supportActionBar?.title = contactName

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Sesión no activa", Toast.LENGTH_SHORT).show()
            activity?.onBackPressedDispatcher?.onBackPressed()
            return
        }

        currentUserId = currentUser.uid
        currentUserName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Usuario"

        // Calcular ID único del chat (ordenado alfabéticamente)
        chatId = if (currentUserId < contactUid) {
            "${currentUserId}_${contactUid}"
        } else {
            "${contactUid}_${currentUserId}"
        }

        activeChatId = chatId

        setupRecyclerView()
        listenForMessages()
        setupListeners()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId)
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // Empezar la visualización desde abajo
            }
            adapter = messageAdapter
        }
    }

    private fun listenForMessages() {
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                val binding = _binding ?: return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(context, "Error al escuchar mensajes: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    messageAdapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                        
                        // Si hay mensajes no leídos del otro usuario, marcarlos como leídos
                        val hasUnread = messages.any { it.senderId != currentUserId && !it.seen }
                        if (hasUnread) {
                            markMessagesAsRead()
                        }
                    }
                }
            }
    }

    private fun markMessagesAsRead() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("seen", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && _binding != null) {
                    val batch = db.batch()
                    var hasUpdates = false
                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId") ?: ""
                        if (senderId != currentUserId) {
                            batch.update(doc.reference, "seen", true)
                            hasUpdates = true
                        }
                    }
                    if (hasUpdates) {
                        batch.commit().addOnFailureListener { e ->
                            Log.e("ChatFragment", "Error al marcar mensajes como leídos", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "Error consultando mensajes no leídos", e)
            }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun sendMessage(text: String) {
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            senderName = currentUserName,
            text = text,
            timestamp = Timestamp.now()
        )

        binding.etMessage.setText("")

        // 1. Guardar mensaje en Firestore
        db.collection("chats").document(chatId)
            .collection("messages")
            .document(messageId)
            .set(message)
            .addOnFailureListener { error ->
                val errorMessage = error.message ?: ""
                if (errorMessage.contains("database", ignoreCase = true) && errorMessage.contains("does not exist", ignoreCase = true)) {
                    showDatabaseNotFoundErrorDialog()
                } else {
                    Toast.makeText(context, "Error al enviar mensaje: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }

        // 2. Escribir alerta de notificación para el destinatario
        val notificationData = mapOf(
            "title" to "Nuevo mensaje de $currentUserName",
            "body" to text,
            "timestamp" to Timestamp.now(),
            "chatId" to chatId,
            "senderId" to currentUserId,
            "senderName" to currentUserName,
            "type" to "CHAT_MESSAGE"
        )

        db.collection("users").document(contactUid)
            .collection("notifications")
            .document(messageId) // Usar el mismo ID para evitar duplicados
            .set(notificationData)

        // 3. Obtener el token de FCM del destinatario y enviar notificación push en segundo plano
        db.collection("users").document(contactUid).get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken") ?: ""
                if (fcmToken.isNotEmpty()) {
                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val fcmPayload = mapOf(
                            "notificationId" to messageId,
                            "type" to "CHAT_MESSAGE",
                            "title" to "Nuevo mensaje de $currentUserName",
                            "body" to text,
                            "senderId" to currentUserId,
                            "senderName" to currentUserName
                        )
                        FcmSender.sendNotification(fcmToken, fcmPayload)
                    }
                } else {
                    Log.d("ChatFragment", "El destinatario no tiene un token de FCM registrado.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "Error al recuperar token de FCM del destinatario", e)
            }
    }

    private fun showDatabaseNotFoundErrorDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Base de Datos No Encontrada")
            .setMessage("La base de datos Firestore (default) no existe en tu proyecto de Firebase (signlink-2acca).\n\nPor favor, ingresa a la consola de Firebase, ve a la sección de 'Firestore Database' y haz clic en 'Crear base de datos' para habilitarla.")
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        if (activeChatId == chatId) {
            activeChatId = null
        }
        messagesListener?.remove()
        messagesListener = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var activeChatId: String? = null
    }
}

class MessageAdapter(
    private val currentUserId: String
) : androidx.recyclerview.widget.ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = if (viewType == TYPE_SENT) {
            inflater.inflate(R.layout.item_message_sent, parent, false)
        } else {
            inflater.inflate(R.layout.item_message_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText = itemView.findViewById<TextView>(R.id.tv_message_text)
        private val ivTicks = itemView.findViewById<android.widget.ImageView>(R.id.iv_status_ticks)

        fun bind(message: Message) {
            tvText.text = message.text
            if (ivTicks != null) {
                if (message.seen) {
                    ivTicks.setImageResource(R.drawable.ic_check_double)
                    ivTicks.setColorFilter(android.graphics.Color.parseColor("#00E5FF"))
                } else {
                    ivTicks.setImageResource(R.drawable.ic_check)
                    ivTicks.setColorFilter(android.graphics.Color.parseColor("#B0FFFFFF"))
                }
            }
        }
    }

    class MessageDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.messageId == newItem.messageId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
    }
}
