package com.maayn.mealmate.data.remote.firebase.syncingDaos

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SyncingMealDao"

class SyncingMealDao(
    private val mealDao: MealDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : MealDao {

    private fun userMealsCollection() =
        firestore.collection("users").document(userId ?: "").collection("meals")

    private var listenerRegistration: ListenerRegistration? = null

    init {
        // Register listener but keep reference to unregister later
        startListeningToMealUpdates()
    }

    private fun startListeningToMealUpdates() {
        // Only start if not already listening
        if (listenerRegistration == null && userId != null) {
            listenerRegistration = userMealsCollection().addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for meal updates", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val meals = snapshot.documents.mapNotNull {
                        it.toObject(Meal::class.java)
                    }

                    if (meals.isNotEmpty()) {
                        // Launch a coroutine to call the suspend function
                        CoroutineScope(Dispatchers.IO).launch {
                            mealDao.insertMeals(meals)
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    override suspend fun insertMealsWithDetails(meals: List<MealWithDetails>) {
        // Insert locally first
        mealDao.insertMealsWithDetails(meals)

        // Then batch upload to Firebase
        withContext(Dispatchers.IO) {
            try {
                if (meals.isNotEmpty()) {
                    val batch = firestore.batch()
                    meals.forEach { meal ->
                        val docRef = userMealsCollection().document(meal.meal.id)
                        batch.set(docRef, meal)
                    }
                    batch.commit().await()
                } else{
                    Log.d(TAG, "No meals to upload")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error batch uploading meals with details", e)
            }
        }
    }

    override suspend fun insertMealWithDetails(meal: MealWithDetails) {
        // Insert locally first
        mealDao.insertMealWithDetails(meal)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(meal.meal.id).set(meal).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading meal with details", e)
            }
        }
    }

    override suspend fun insertMeal(meal: Meal) {
        // Insert locally first
        mealDao.insertMeal(meal)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(meal.id).set(meal).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading meal", e)
            }
        }
    }

    override suspend fun insertMeals(meals: List<Meal>) {
        // Insert locally first
        mealDao.insertMeals(meals)

        // Then batch upload to Firebase
        withContext(Dispatchers.IO) {
            try {
                if (meals.isNotEmpty()) {
                    val batch = firestore.batch()
                    meals.forEach { meal ->
                        if (meal.id.isNotEmpty()) {
                            val docRef = userMealsCollection().document(meal.id)
                            batch.set(docRef, meal)
                        }
                    }
                    batch.commit().await()
                } else {
                    Log.d(TAG, "No meals to upload")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error batch uploading meals", e)
            }
        }
    }

    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        mealDao.insertIngredients(ingredients)
    }

    override suspend fun insertInstructions(instructions: List<InstructionEntity>) {
        mealDao.insertInstructions(instructions)
    }

    // Get methods only use local data, as we rely on the listener for updates
    override suspend fun getAllMeals(): List<Meal> {
        return mealDao.getAllMeals()
    }

    override suspend fun getMealWithDetails(mealId: String): MealWithDetails {
        return mealDao.getMealWithDetails(mealId)
    }

    override suspend fun getAllMealWithDetails(): List<MealWithDetails> {
        return mealDao.getAllMealWithDetails()
    }

    override suspend fun getMealsWithDetailsByCategory(category: String): List<MealWithDetails> {
        return mealDao.getMealsWithDetailsByCategory(category)
    }

    override suspend fun getMealsWithDetailsByArea(area: String): List<MealWithDetails> {
        return mealDao.getMealsWithDetailsByArea(area)
    }

    override suspend fun getMealsWithDetailsByIngredient(ingredient: String): List<MealWithDetails> {
        return mealDao.getMealsWithDetailsByIngredient(ingredient)
    }

    override suspend fun getMealById(mealId: String): Meal? {
        return mealDao.getMealById(mealId)
    }

    override suspend fun deleteMeal(mealId: String) {
        // Delete locally first
        mealDao.deleteMeal(mealId)

        // Then delete from Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(mealId).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting meal", e)
            }
        }
    }

    override suspend fun deleteMealIngredients(mealId: String) {
        mealDao.deleteMealIngredients(mealId)
    }

    override suspend fun deleteMealInstructions(mealId: String) {
        mealDao.deleteMealInstructions(mealId)
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = userMealsCollection().get().await()
                val meals = snapshot.documents.mapNotNull {
                    it.toObject(Meal::class.java)
                }

                if (meals.isNotEmpty()) {
                    mealDao.insertMeals(meals)
                    Log.d(TAG, "Synced ${meals.size} meals from Firebase")
                } else {
                    Log.d(TAG, "No meals to sync from Firebase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from Firebase", e)
            }
        }
    }
}