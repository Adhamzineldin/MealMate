package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.FavoriteMealDao
import com.maayn.mealmate.data.local.entities.FavoriteMeal
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class SyncingFavoriteMealDao(
    private val favoriteMealDao: FavoriteMealDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : FavoriteMealDao {

    private fun userMealsCollection() =
        firestore.collection("users").document(userId.toString()).collection("favorite_meals")

    override suspend fun insertMeal(meal: Meal) {
        favoriteMealDao.insertMeal(meal) // Save locally

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(meal.id).set(meal).await() // Store under user
                Log.d("SyncingFavoriteMealDao", "Meal successfully inserted to Firestore: ${meal.id}")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error inserting meal to Firestore: ${e.message}")
            }
        }
    }



    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        favoriteMealDao.insertIngredients(ingredients)
    }

    override suspend fun insertInstructions(instructions: List<InstructionEntity>) {
        favoriteMealDao.insertInstructions(instructions)
    }

    override suspend fun insertFavoriteMeal(favoriteMeal: FavoriteMeal) {
        // Ensure the ID is valid (add this check)
        Log.d("SyncingFavoriteMealDao", "Favorite Meal successfully inserted locally: ${favoriteMeal}")
        if (favoriteMeal.id.isBlank()) {
            Log.e("SyncingFavoriteMealDao", "Cannot insert favorite meal with empty ID")
            return
        }

        favoriteMealDao.insertFavoriteMeal(favoriteMeal) // Save locally


        try {
            userMealsCollection().document(favoriteMeal.id).set(favoriteMeal).await()
            Log.d("SyncingFavoriteMealDao", "Favorite Meal successfully inserted to Firestore: ${favoriteMeal.id}")
        } catch (e: Exception) {
            Log.e("SyncingFavoriteMealDao", "Error inserting favorite meal to Firestore: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun updateMeal(meal: Meal) {
        favoriteMealDao.updateMeal(meal)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(meal.id).set(meal).await() // Update under user
                Log.d("SyncingFavoriteMealDao", "Meal successfully updated in Firestore: ${meal.id}")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error updating meal in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun updateIngredients(ingredients: List<IngredientEntity>) {
        favoriteMealDao.updateIngredients(ingredients)
    }

    override suspend fun updateInstructions(instructions: List<InstructionEntity>) {
        favoriteMealDao.updateInstructions(instructions)
    }

    override suspend fun updateFavoriteMeal(favoriteMeal: FavoriteMeal) {
        favoriteMealDao.updateFavoriteMeal(favoriteMeal)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(favoriteMeal.id).set(favoriteMeal).await() // Update under user
                Log.d("SyncingFavoriteMealDao", "Favorite Meal successfully updated in Firestore: ${favoriteMeal.id}")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error updating favorite meal in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.insertMealWithDetails(mealWithDetails)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails).await()
                Log.d("SyncingFavoriteMealDao", "Meal with details successfully inserted to Firestore: ${mealWithDetails.meal.id}")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error inserting meal with details to Firestore: ${e.message}")
            }
        }
    }

    override suspend fun updateMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.updateMealWithDetails(mealWithDetails)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails).await()
                Log.d("SyncingFavoriteMealDao", "Meal with details successfully updated in Firestore: ${mealWithDetails.meal.id}")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error updating meal with details in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun getAllFavoriteMealDetails(): List<MealWithDetails> {
        return favoriteMealDao.getAllFavoriteMealDetails()
    }

    override suspend fun getFavoriteMealDetailsById(mealId: String): MealWithDetails? {
        return favoriteMealDao.getFavoriteMealDetailsById(mealId)
    }

    override suspend fun deleteFavoriteMeal(mealId: String) {
        favoriteMealDao.deleteFavoriteMeal(mealId)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(mealId).delete().await() // Delete only for the user
                Log.d("SyncingFavoriteMealDao", "Favorite Meal successfully deleted from Firestore: $mealId")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error deleting favorite meal from Firestore: $e.message")
            }
        }
    }

    override suspend fun updateMealAsNotFavorite(mealId: String) {
        favoriteMealDao.updateMealAsNotFavorite(mealId)
    }

    override suspend fun removeFromFavorites(mealId: String) {
        favoriteMealDao.removeFromFavorites(mealId)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userMealsCollection().document(mealId).delete().await()
                Log.d("SyncingFavoriteMealDao", "Meal successfully removed from favorites in Firestore: $mealId")
            } catch (e: Exception) {
                Log.e("SyncingFavoriteMealDao", "Error removing meal from favorites in Firestore: $e.message")
            }
        }
    }

    suspend fun syncFromFirebase() {
        try {
            if (userId == null) {
                Log.e("SyncingFavoriteMealDao", "User not authenticated")
                return
            }

            val favoriteMealSnapshot = userMealsCollection().get().await()
            if (favoriteMealSnapshot.isEmpty) {
                Log.w("SyncingFavoriteMealDao", "No favorite meals found")
                return
            }

            val favoriteMeals: List<FavoriteMeal> = favoriteMealSnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val favoriteMeal = doc.toObject(FavoriteMeal::class.java)?.copy(id = data?.get("id")
                    .toString()) // 🔹 Set the Firestore doc ID
                if (favoriteMeal == null) {
                    Log.e("SyncingFavoriteMealDao", "Failed to parse document: ${doc.id}")
                    null
                } else {
                    favoriteMeal
                }
            }

            favoriteMeals.forEach { favoriteMeal ->
                Log.d("SyncingFavoriteMealDao", "Inserting favorite meal locally: $favoriteMeal")
                if (favoriteMeal.id != null){
                    favoriteMealDao.insertFavoriteMeal(favoriteMeal)
                }

            }

        } catch (e: Exception) {
            Log.e("SyncingFavoriteMealDao", "Error syncing favorite meals: ${e.message}")
            e.printStackTrace()
        }
    }
}
