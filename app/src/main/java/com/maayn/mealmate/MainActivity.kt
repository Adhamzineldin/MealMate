package com.maayn.mealmate


import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.maayn.mealmate.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var lastClickTime: Long = 0
    private val debounceTime = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make status bar icons dark (for better visibility on white background)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR


        // Reference the NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Set up BottomNavigationView with NavController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Handle navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigateSafely(R.id.homeFragment)
                    true
                }
                R.id.nav_meal_plan -> {
//                    navController.navigate(R.id.mealPlanFragment)
                    true
                }
                R.id.nav_shop -> {
//                    navController.navigate(R.id.shopFragment)
                    true
                }
                R.id.nav_recipes -> {
//                    navController.navigate(R.id.recipesFragment)
                    true
                }
                R.id.nav_profile -> {
                    val firebaseAuth = FirebaseAuth.getInstance()

                    if (firebaseAuth.currentUser != null) {
                        // User is logged in, navigate to the profile fragment
                        navController.navigateSafely(R.id.profileFragment)
                    } else {
                        // User is not logged in, navigate to the login fragment
                        navController.navigateSafely(R.id.loginFragment)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Reference the NavController for handling bottom navigation visibility
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Add DestinationChangedListener to show/hide bottom navigation
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signupFragment -> binding.bottomNavigationFragment.visibility = View.GONE
                else -> binding.bottomNavigationFragment.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        if (navController.currentDestination?.id == R.id.homeFragment) {
            // Exit app instead of going back to login
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    fun NavController.navigateSafely(destinationId: Int) {
        // Prevent navigation to the same destination
        if (currentDestination?.id != destinationId) {
            navigate(destinationId)
        }
    }



}
