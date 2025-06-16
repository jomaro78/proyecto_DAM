package com.montilivi.esdeveniments.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.databinding.ActivityMainBinding
import com.montilivi.esdeveniments.utils.FirebaseReferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // mode dia/nit
        val mode = getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
        actualizarBotonTema()

        // toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        // gestió del drawer
        val isOrganizer = getSharedPreferences("user", Context.MODE_PRIVATE)
            .getBoolean("isOrganizer", false)
        binding.navView.menu.findItem(R.id.nav_create_event)?.isVisible = isOrganizer
        binding.navView.menu.findItem(R.id.nav_suggest_category)?.isVisible = isOrganizer

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val bottomNav: BottomNavigationView = binding.bottomNav

        // navegació
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.subscribedEventsFragment, R.id.pastEventsFragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // restriccions de drawer i bottomNav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val visibleScreens = setOf(
                R.id.homeFragment,
                R.id.subscribedEventsFragment,
                R.id.pastEventsFragment
            )

            if (destination.id in visibleScreens) {
                binding.bottomNav.visibility = View.VISIBLE
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            } else {
                binding.bottomNav.visibility = View.GONE
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                binding.bottomNav.menu.setGroupCheckable(0, true, false)
                for (i in 0 until binding.bottomNav.menu.size()) {
                    binding.bottomNav.menu.getItem(i).isChecked = false
                }
                binding.bottomNav.menu.setGroupCheckable(0, true, true)
            }

            // actualització títol toolbar
            supportActionBar?.apply {
                setDisplayShowCustomEnabled(false)
                customView = null
                setDisplayShowTitleEnabled(true)
            }
        }

        // nvegació bottomNav amb neteja de la pila
        bottomNav.setOnItemSelectedListener { item ->
            val popUpToId = item.itemId
            navController.navigate(item.itemId, null, navOptions {
                popUpTo(popUpToId) { inclusive = true }
            })
            true
        }

        // verificació d'autenticació i navegació segons estat
        binding.root.post {
            val currentUser = FirebaseReferences.auth.currentUser

            if (currentUser == null) {
                navController.navigate(R.id.loginFragment)
                mostrarContenido()
            } else {
                val userId = currentUser.uid
                lifecycleScope.launch {
                    try {
                        val doc = FirebaseReferences.usersCollection.document(userId).get().await()
                        if (!doc.exists()) {
                            navController.navigate(R.id.preferencesFragment)
                        } else {
                            navController.navigate(R.id.homeFragment)
                        }
                    } catch (e: Exception) {
                        navController.navigate(R.id.loginFragment)
                    } finally {
                        mostrarContenido()
                    }
                }
            }
        }

        // navegació del drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(R.id.loginFragment)
                    true
                }
                R.id.nav_create_event -> {
                    navController.navigate(R.id.createEventFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_preferences -> {
                    navController.navigate(R.id.preferencesFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_suggest_category -> {
                    navController.navigate(R.id.suggestCategoryFragment)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_toggle_theme -> {
                    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    val newMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
                        AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES

                    AppCompatDelegate.setDefaultNightMode(newMode)
                    getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                        .putInt("theme_mode", newMode)
                        .apply()
                    actualizarBotonTema()
                    recreate()
                    true
                }
                else -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    false
                }
            }
        }

        verificarRolYActualizarMenu()
        actualizarHeaderDrawer()
    }

    private fun mostrarContenido() {
        findViewById<View>(R.id.mainContent).visibility = View.VISIBLE
    }

    fun verificarRolYActualizarMenu() {
        val currentUser = FirebaseReferences.auth.currentUser
        currentUser?.uid?.let { userId ->
            lifecycleScope.launch {
                try {
                    val document = FirebaseReferences.usersCollection.document(userId).get().await()
                    val isOrganizer = document.getBoolean("isOrganizer") ?: false

                    getSharedPreferences("user", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("isOrganizer", isOrganizer)
                        .apply()

                    binding.navView.menu.findItem(R.id.nav_create_event)?.isVisible = isOrganizer
                    binding.navView.menu.findItem(R.id.nav_suggest_category)?.isVisible = isOrganizer
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error obteniendo rol del usuario", e)
                }
            }
        }
    }

    fun actualizarHeaderDrawer() {
        val headerView = binding.navView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.ivDrawerProfileImage)
        val nameView = headerView.findViewById<TextView>(R.id.tvDrawerUserName)

        FirebaseReferences.auth.currentUser?.uid?.let { userId ->
            lifecycleScope.launch {
                try {
                    val doc = FirebaseReferences.usersCollection.document(userId).get().await()
                    val username = doc.getString("username") ?: doc.getString("email") ?: "Usuari"
                    nameView.text = username

                    val imageUrl = doc.getString("profileImageUri")
                    Glide.with(this@MainActivity)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(imageView)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error al cargar datos del usuario", e)
                }
            }
        }
    }

    private fun actualizarBotonTema() {
        val item = binding.navView.menu.findItem(R.id.nav_toggle_theme)
        val mode = AppCompatDelegate.getDefaultNightMode()

        when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> {
                item.title = getString(R.string.night_mode)
                item.setIcon(R.drawable.ic_moon)
            }
            AppCompatDelegate.MODE_NIGHT_NO -> {
                item.title = getString(R.string.day_mode)
                item.setIcon(R.drawable.ic_sun)
            }
            else -> {
                item.title = getString(R.string.system_theme)
                item.setIcon(R.drawable.ic_system)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // amagar el teclat quan pulsem fora d'ell
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            currentFocus?.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }
}
