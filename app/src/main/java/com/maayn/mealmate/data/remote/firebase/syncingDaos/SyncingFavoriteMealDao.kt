package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.FavoriteMealDao
import com.maayn.mealmate.data.local.database.AppDatabase
import com.maayn.mealmate.data.local.entities.FavoriteMeal
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingFavoriteMealDao(
    private val favoriteMealDao: FavoriteMealDao,
    private val firestore: FirebaseFirestore
) : FavoriteMealDao {

    override suspend fun insertMeal(meal: Meal) {
        favoriteMealDao.insertMeal(meal) // Save locally

        // Sync to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meals").document().set(meal)
        }
    }


    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        favoriteMealDao.insertIngredients(ingredients) // Save locally
    }

    override suspend fun insertInstructions(instructions: List<InstructionEntity>) {
        favoriteMealDao.insertInstructions(instructions) // Save locally
    }

    override suspend fun insertFavoriteMeal(favoriteMeal: FavoriteMeal) {
        favoriteMealDao.insertFavoriteMeal(favoriteMeal) // Save locally

        // Sync to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("favorite_meals").document(favoriteMeal.id).set(favoriteMeal)
        }
    }

    override suspend fun updateMeal(meal: Meal) {
        favoriteMealDao.updateMeal(meal) // Update locally

        // Sync update to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meals").document(meal.id).set(meal)
        }
    }

    override suspend fun updateIngredients(ingredients: List<IngredientEntity>) {
        favoriteMealDao.updateIngredients(ingredients) // Update locally
    }

    override suspend fun updateInstructions(instructions: List<InstructionEntity>) {
        favoriteMealDao.updateInstructions(instructions) // Update locally
    }

    override suspend fun updateFavoriteMeal(favoriteMeal: FavoriteMeal) {
        favoriteMealDao.updateFavoriteMeal(favoriteMeal) // Update locally

        // Sync update to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("favorite_meals").document(favoriteMeal.id).set(favoriteMeal)
        }
    }

    override suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.insertMealWithDetails(mealWithDetails) // Save locally

        // Sync to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meals").document(mealWithDetails.meal.id).set(mealWithDetails)
            firestore.collection("favorite_meals").document(mealWithDetails.meal.id).set(
                FavoriteMeal(id = mealWithDetails.meal.id)
            )
        }
    }

    override suspend fun updateMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.updateMealWithDetails(mealWithDetails) // Update locally

        // Sync update to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("meals").document(mealWithDetails.meal.id).set(mealWithDetails)
            firestore.collection("favorite_meals").document(mealWithDetails.meal.id).set(
                FavoriteMeal(id = mealWithDetails.meal.id)
            )
        }
    }

    override suspend fun getAllFavoriteMealDetails(): List<MealWithDetails> {
        return favoriteMealDao.getAllFavoriteMealDetails() // Local fetch
    }

    override suspend fun getFavoriteMealDetailsById(mealId: String): MealWithDetails? {
        return favoriteMealDao.getFavoriteMealDetailsById(mealId) // Local fetch
    }

    override suspend fun deleteFavoriteMeal(mealId: String) {
        favoriteMealDao.deleteFavoriteMeal(mealId) // Delete locally

        // Remove from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("favorite_meals").document(mealId).delete()
        }
    }

    override suspend fun updateMealAsNotFavorite(mealId: String) {
        favoriteMealDao.updateMealAsNotFavorite(mealId) // Update locally
    }

    override suspend fun removeFromFavorites(mealId: String) {
        favoriteMealDao.removeFromFavorites(mealId) // Update locally

        // Remove from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("favorite_meals").document(mealId).delete()
        }
    }

    suspend fun syncFromFirebase() {
        try {
            // Fetch favorite meals from Firestore
            val favoriteMealSnapshot = firestore.collection("favorite_meals").get().await()
            val favoriteMeals: List<FavoriteMeal> = favoriteMealSnapshot.documents.mapNotNull { doc ->
                doc.toObject(FavoriteMeal::class.java)
            }

            // ✅ Insert favorite meals into local database (only this, no meals or ingredients)
            favoriteMeals.forEach { favoriteMeal ->
                favoriteMealDao.insertFavoriteMeal(favoriteMeal)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }







}
