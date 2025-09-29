package com.example.organizer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.organizer.R
import com.example.organizer.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val drawerLayout: DrawerLayout? = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.addEventFragment,
                R.id.consultFragment,
                R.id.backupFragment,
                R.id.aboutFragment
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.addEventFragment -> {
                    navController.navigate(R.id.addEventFragment)
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.consultFragment -> {
                    navController.navigate(R.id.consultFragment)
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.backupFragment -> {
                    navController.navigate(R.id.backupFragment)
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.aboutFragment -> {
                    navController.navigate(R.id.aboutFragment)
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.exit_app -> {
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}