package com.maayn.mealmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingFavoriteMealDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingIngredientDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealOfTheDayDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingMealPlanDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingShoppingItemDao
import com.maayn.mealmate.data.remote.firebase.syncingDaos.SyncingShoppingListDao
import com.maayn.mealmate.databinding.ActivityMainBinding
import com.maayn.mealmate.presentation.splash.LoadingFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, you can now post notifications
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, show an appropriate message
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permission for notifications if on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupFirestore()
        setupNavigation()
        observeLoginState()
    }

    // 🔹 Setup Firestore with offline persistence
    private fun setupFirestore() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
    }

    // 🔹 Setup Navigation and Bottom Navigation
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        bottomNav.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_meal_plan -> if (firebaseAuth.currentUser != null) R.id.mealPlanFragment else R.id.loginFragment
                R.id.nav_favorites -> if (firebaseAuth.currentUser != null) R.id.favoritesFragment else R.id.loginFragment
                R.id.nav_recipes -> R.id.recipesFragment
                R.id.nav_profile -> if (firebaseAuth.currentUser != null) R.id.profileFragment else R.id.loginFragment
                else -> null
            }
            destination?.let { navController.navigateSafely(it) }
            destination != null
        }

        handleBottomNavVisibility()
    }

    private fun observeLoginState() {
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                syncDataFromFirestore()
            }
        }

        firebaseAuth.addAuthStateListener(authListener)
    }


    // 🔹 Sync data from Firestore to local Room database
    private fun syncDataFromFirestore() {
        val loadingFragment = LoadingFragment()
        loadingFragment.show(supportFragmentManager, "loading")

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val firestore = FirebaseFirestore.getInstance()

            val mealDao = db.mealDao()
            val favoriteMealDao = db.favoriteMealDao()
            val mealPlanDao = db.mealPlanDao()
            val ingredientDao = db.ingredientDao()
            val mealOfTheDayDao = db.mealOfTheDayDao()
            val shoppingItemDao = db.shoppingItemDao()

            val syncingMealDao = SyncingMealDao(mealDao, firestore)
            val syncingFavoriteMealDao = SyncingFavoriteMealDao(favoriteMealDao, firestore)
            val syncingMealPlanDao = SyncingMealPlanDao(mealPlanDao, firestore)
            val syncingIngredientDao = SyncingIngredientDao(ingredientDao, firestore)
            val syncingMealOfTheDayDao = SyncingMealOfTheDayDao(mealOfTheDayDao, firestore)
            val syncingShoppingItemDao = SyncingShoppingItemDao(shoppingItemDao, firestore)

            // Sync all DAOs from Firestore to Room
            syncingMealDao.syncFromFirebase()
            syncingIngredientDao.syncFromFirebase()
            syncingFavoriteMealDao.syncFromFirebase()
            syncingMealPlanDao.syncMealPlansFromFirebase()
            syncingShoppingItemDao.syncFromFirebase()
            syncingMealOfTheDayDao.syncFromFirebase()

            // Dismiss loading screen after sync completes
            launch(Dispatchers.Main) {
                loadingFragment.dismiss()
            }
        }
    }



    // 🔹 Manage Bottom Navigation visibility based on destination
    private fun handleBottomNavVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val layoutParams = binding.navHostFragment.layoutParams as ViewGroup.MarginLayoutParams
            if (destination.id in listOf(R.id.loginFragment, R.id.signupFragment)) {
                layoutParams.bottomMargin = 0
                binding.bottomNavigationFragment.visibility = View.GONE
            } else {
                layoutParams.bottomMargin = (85 * resources.displayMetrics.density).toInt()
                binding.bottomNavigationFragment.visibility = View.VISIBLE
            }
            binding.navHostFragment.layoutParams = layoutParams
        }
    }

    // 🔹 Handle Back Press
    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.homeFragment) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    // 🔹 Handle Navigation Up
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // 🔹 Prevent duplicate navigation calls
    private fun NavController.navigateSafely(destinationId: Int) {
        if (currentDestination?.id != destinationId) navigate(destinationId)
    }
}
