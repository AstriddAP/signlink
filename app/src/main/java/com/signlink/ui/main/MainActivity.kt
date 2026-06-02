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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
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
                R.id.nav_map, R.id.nav_settings
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        
        // Actualizar datos del usuario en el Header del Drawer
        val headerView = binding.navView.getHeaderView(0)
        val navName = headerView.findViewById<android.widget.TextView>(R.id.nav_header_name)
        val navEmail = headerView.findViewById<android.widget.TextView>(R.id.nav_header_email)

        lifecycleScope.launch {
            authViewModel.userProfile.collectLatest { user ->
                user?.let {
                    navName.text = it.displayName
                    navEmail.text = it.email
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
                binding.appBarMain.contentMain.bottomNav.visibility = View.VISIBLE
                binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        handleSharedAudio(navController)
    }

    private fun handleSharedAudio(navController: androidx.navigation.NavController) {
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            (intent.getParcelableExtra<android.os.Parcelable>(android.content.Intent.EXTRA_STREAM) as? android.net.Uri)?.let { uri ->
                val bundle = Bundle().apply {
                    putString("audio_uri", uri.toString())
                }
                navController.navigate(R.id.nav_audio_transcription, bundle)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
