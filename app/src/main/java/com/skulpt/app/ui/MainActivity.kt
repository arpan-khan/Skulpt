package com.skulpt.app.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.skulpt.app.R
import com.skulpt.app.SkulptApplication
import com.skulpt.app.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.workoutSessionFragment || destination.id == R.id.customDayFragment) {
                bottomNav.menu.findItem(R.id.dashboardFragment).isChecked = true
            }
        }

        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.dashboardFragment) {
                val currentDestination = navController.currentDestination?.id
                if (currentDestination == R.id.workoutSessionFragment || currentDestination == R.id.customDayFragment) {

                    navController.popBackStack(R.id.dashboardFragment, false)
                }
            }
        }
    }
}
