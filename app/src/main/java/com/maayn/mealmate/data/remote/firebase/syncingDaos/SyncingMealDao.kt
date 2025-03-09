package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.MealDao
import com.maayn.mealmate.data.local.entities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SyncingMealDao(
    private val mealDao: MealDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid // Default to current user if null

) : MealDao {

    private fun userMealsCollection() =
        firestore.collection("users").document(userId.toString()).collection("meals")

    init {
        listenToMealUpdates() // ✅ Start real-time sync
    }

    // 🔹 **INSERT METHODS**
    override suspend fun insertMealsWithDetails(meals: List<MealWithDetails>) {
        mealDao.insertMealsWithDetails(meals) // Save locally
        CoroutineScope(Dispatchers.IO).launch { syncMealsToFirebase(meals) }
    }

    override suspend fun insertMealWithDetails(meal: MealWithDetails) {
        mealDao.insertMealWithDetails(meal)
        CoroutineScope(Dispatchers.IO).launch { syncMealToFirebase(meal) }
    }

    override suspend fun insertMeal(meal: Meal) {
        mealDao.insertMeal(meal)
        CoroutineScope(Dispatchers.IO).launch {
            userMealsCollection().document(meal.id).set(meal) // ✅ Store under user
        }
    }

    override suspend fun insertMeals(meals: List<Meal>) {
        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            meals.forEach { meal ->
                if (meal.id.isNotEmpty()) {
                    val docRef = userMealsCollection().document(meal.id)
                    batch.set(docRef, meal)
                }
            }
            batch.commit()
        }
    }

    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        mealDao.insertIngredients(ingredients)
    }

    override suspend fun insertInstructions(instructions: List<InstructionEntity>) {
        mealDao.insertInstructions(instructions)
    }

    // 🔹 **GET METHODS**
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

    // 🔹 **DELETE METHODS**
    override suspend fun deleteMeal(mealId: String) {
        mealDao.deleteMeal(mealId)
        CoroutineScope(Dispatchers.IO).launch { userMealsCollection().document(mealId).delete() }
    }

    override suspend fun deleteMealIngredients(mealId: String) {
        mealDao.deleteMealIngredients(mealId)
    }

    override suspend fun deleteMealInstructions(mealId: String) {
        mealDao.deleteMealInstructions(mealId)
    }

    // 🔹 **SYNC FROM FIREBASE TO ROOM**
    suspend fun syncFromFirebase() {
        try {
            val snapshot = userMealsCollection().get().await()
            val meals = snapshot.documents.mapNotNull { it.toObject(Meal::class.java) }

            if (meals.isNotEmpty()) {
                mealDao.insertMeals(meals) // ✅ Save meals to Room
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔹 **Real-time Sync (Listen for Firestore Changes)**
    private fun listenToMealUpdates() {
        userMealsCollection().addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            CoroutineScope(Dispatchers.IO).launch {
                val meals = snapshot.documents.mapNotNull { it.toObject(Meal::class.java) }
                mealDao.insertMeals(meals) // ✅ Sync updates locally
            }
        }
    }

    // 🔹 **SYNC TO FIREBASE**
    private suspend fun syncMealsToFirebase(meals: List<MealWithDetails>) {
        val batch = firestore.batch()
        meals.forEach { meal ->
            val mealId = meal.meal.id.takeIf { it.isNotEmpty() } ?: return@forEach
            val docRef = userMealsCollection().document(mealId)
            batch.set(docRef, meal)
        }
        batch.commit()
    }

    private suspend fun syncMealToFirebase(meal: MealWithDetails) {
        userMealsCollection().document(meal.meal.id).set(meal).await()
    }
}
