package com.signlink.ui.main

import android.content.pm.PackageManager
import android.util.Log
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.signlink.R
import com.signlink.databinding.ActivityMainBinding
import com.signlink.ui.onboarding.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()
    private var notificationListener: com.google.firebase.firestore.ListenerRegistration? = null

    @Inject
    lateinit var userRepository: com.signlink.data.repository.UserRepository

    private val requestPermissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("PERMISSIONS", "${it.key} = ${it.value}")
        }
        val postNotificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }
        if (postNotificationsGranted) {
            fetchAndSaveFcmToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cargar y aplicar de inmediato el modo oscuro guardado antes de inflar layouts
        val prefs = getSharedPreferences("signlink_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        
        // Debug API Key
        try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            val apiKey = bundle.getString("com.google.android.geo.API_KEY")
            Log.d("MAP_DEBUG", "API Key en Manifest: $apiKey")
        } catch (e: Exception) {
            Log.e("MAP_DEBUG", "Error leyendo API Key", e)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Navigation Drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_communicate,
                R.id.nav_live_captioning, R.id.nav_settings,
                R.id.nav_ai_explanation, R.id.nav_audio_transcription,
                R.id.nav_dictionary, R.id.nav_contacts
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        
        // Actualizar datos del usuario en el Header del Drawer
        val headerView = binding.navView.getHeaderView(0)
        val navName = headerView.findViewById<android.widget.TextView>(R.id.nav_header_name)
        val navEmail = headerView.findViewById<android.widget.TextView>(R.id.nav_header_email)

        // Inicializar inmediatamente con los datos locales del usuario logueado en Firebase Auth
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val name = currentUser.displayName
            val email = currentUser.email
            navName.text = if (!name.isNullOrEmpty()) name else email?.substringBefore("@") ?: "Usuario"
            navEmail.text = email ?: ""
        } else {
            navName.text = "SignLink User"
            navEmail.text = "user@signlink.com"
        }

        lifecycleScope.launch {
            authViewModel.userProfile.collectLatest { user ->
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    navName.text = if (user.displayName.isNotEmpty()) user.displayName else user.email.substringBefore("@")
                    navEmail.text = user.email
                    checkAndRequestAllPermissions()
                    startFirestoreNotificationListener(user.uid)
                } else if (firebaseUser != null) {
                    val name = firebaseUser.displayName
                    val email = firebaseUser.email
                    navName.text = if (!name.isNullOrEmpty()) name else email?.substringBefore("@") ?: "Usuario"
                    navEmail.text = email ?: ""
                    checkAndRequestAllPermissions()
                    startFirestoreNotificationListener(firebaseUser.uid)
                } else {
                    stopFirestoreNotificationListener()
                    navName.text = "SignLink User"
                    navEmail.text = "user@signlink.com"
                }
            }
        }

        authViewModel.checkUserStatus()
        
        // Bottom Navigation
        binding.appBarMain.contentMain.bottomNav.setupWithNavController(navController)

        // Custom listener for Drawer to handle Logout specifically
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawers()
                    authViewModel.logout()
                    
                    // Navegación limpia al login eliminando todo el historial anterior
                    navController.navigate(R.id.loginFragment, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .setLaunchSingleTop(true)
                        .build())
                    true
                }
                R.id.nav_home -> {
                    binding.drawerLayout.closeDrawers()
                    navController.navigate(R.id.nav_home, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .setLaunchSingleTop(true)
                        .build())
                    true
                }
                else -> {
                    val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) binding.drawerLayout.closeDrawers()
                    handled
                }
            }
        }

        // Controlar visibilidad de barras según el fragmento
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.splashFragment || destination.id == R.id.loginFragment) {
                binding.appBarMain.toolbar.visibility = View.GONE
                binding.appBarMain.contentMain.bottomNav.visibility = View.GONE
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                binding.appBarMain.toolbar.visibility = View.VISIBLE
                binding.appBarMain.contentMain.bottomNav.visibility = View.GONE
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)

                // Actualizar info del drawer al navegar a pantallas internas
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val name = firebaseUser.displayName
                    val email = firebaseUser.email
                    navName.text = if (!name.isNullOrEmpty()) name else email?.substringBefore("@") ?: "Usuario"
                    navEmail.text = email ?: ""
                } else {
                    navName.text = "SignLink User"
                    navEmail.text = "user@signlink.com"
                }
            }
        }

        handleSharedAudio(navController)
        handleNotificationIntent(navController)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        handleSharedAudio(navController)
        handleNotificationIntent(navController)
    }

    private fun handleSharedAudio(navController: androidx.navigation.NavController) {
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            (intent.getParcelableExtra<android.os.Parcelable>(android.content.Intent.EXTRA_STREAM) as? android.net.Uri)?.let { uri ->
                val bundle = Bundle().apply {
                    putString("audio_uri", uri.toString())
                }
                navController.navigate(R.id.nav_audio_transcription, bundle)
                // Limpiar la acción del intent para evitar re-lanzamientos en recreaciones
                intent.action = null
            }
        }
    }

    private fun handleNotificationIntent(navController: androidx.navigation.NavController) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        if (navigateTo == "chat") {
            val contactUid = intent?.getStringExtra("contact_uid") ?: ""
            val contactName = intent?.getStringExtra("contact_name") ?: "Contacto"
            if (contactUid.isNotEmpty()) {
                val bundle = Bundle().apply {
                    putString("contact_uid", contactUid)
                    putString("contact_name", contactName)
                }
                
                // Si el usuario está autenticado, navega directamente.
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    try {
                        navController.navigate(R.id.nav_chat, bundle)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error al navegar al chat desde notificación", e)
                    }
                }
                
                // Limpiar extras para evitar re-navegación en recreaciones de la Activity
                intent?.removeExtra("navigate_to")
                intent?.removeExtra("contact_uid")
                intent?.removeExtra("contact_name")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkAndRequestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation || !hasCoarseLocation) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasCamera) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }
        if (!hasAudio) {
            permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasNotifications = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotifications) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            fetchAndSaveFcmToken()
        }
    }

    private fun fetchAndSaveFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Token actual de FCM: $token")
            authViewModel.updateFcmToken(token)
        }
    }

    private fun startFirestoreNotificationListener(uid: String) {
        stopFirestoreNotificationListener()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        notificationListener = db.collection("users").document(uid)
            .collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_NOTIF", "Error al escuchar notificaciones", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val type = doc.getString("type")
                            val chatId = doc.getString("chatId") ?: ""
                            
                            if (type == "MUTUAL_CONTACT_ADD") {
                                val contactUid = doc.getString("uid") ?: ""
                                val contactName = doc.getString("displayName") ?: ""
                                val contactEmail = doc.getString("email") ?: ""
                                if (contactUid.isNotEmpty()) {
                                    val newContact = com.signlink.data.model.User(
                                        uid = contactUid,
                                        displayName = contactName,
                                        email = contactEmail
                                    )
                                    lifecycleScope.launch {
                                        userRepository.addContact(uid, newContact)
                                    }
                                }
                            }
                            
                            val title = doc.getString("title") ?: "Nuevo mensaje"
                            val body = doc.getString("body") ?: ""
                            
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: "Contacto"
                            
                            // Si es un mensaje de chat, verificamos si es del chat que el usuario tiene abierto actualmente
                            if (type == "CHAT_MESSAGE" && chatId.isNotEmpty() && chatId == com.signlink.ui.contacts.ChatFragment.activeChatId) {
                                Log.d("MainActivity", "El usuario ya está en el chat activo ($chatId). Se omite el banner de notificación local.")
                            } else {
                                val navigateTo = if (type == "CHAT_MESSAGE" || type == "MUTUAL_CONTACT_ADD") "chat" else null
                                val navUid = if (type == "MUTUAL_CONTACT_ADD") (doc.getString("uid") ?: "") else senderId
                                val navName = if (type == "MUTUAL_CONTACT_ADD") (doc.getString("displayName") ?: "") else senderName
                                triggerLocalNotification(
                                    title = title, 
                                    messageBody = body, 
                                    navigateTo = navigateTo, 
                                    contactUid = navUid, 
                                    contactName = navName
                                )
                            }
                            
                            // Borramos el registro procesado para evitar notificaciones repetidas
                            db.collection("users").document(uid)
                                .collection("notifications")
                                .document(doc.id)
                                .delete()
                        }
                    }
                }
            }
    }

    private fun stopFirestoreNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
    }

    private fun triggerLocalNotification(
        title: String, 
        messageBody: String, 
        navigateTo: String? = null, 
        contactUid: String? = null, 
        contactName: String? = null
    ) {
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (navigateTo != null) {
                putExtra("navigate_to", navigateTo)
                putExtra("contact_uid", contactUid)
                putExtra("contact_name", contactName)
            }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "signlink_notifications"
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Alertas de SignLink",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

}
