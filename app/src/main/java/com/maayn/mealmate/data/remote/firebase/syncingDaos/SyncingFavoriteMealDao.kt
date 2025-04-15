package com.maayn.mealmate.data.remote.firebase.syncingDaos

import android.util.Log
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
            userMealsCollection().document(meal.id).set(meal) // ✅ Store under user
        }
    }

    override suspend fun insertIngredients(ingredients: List<IngredientEntity>) {
        favoriteMealDao.insertIngredients(ingredients)
    }

    override suspend fun insertInstructions(instructions: List<InstructionEntity>) {
        favoriteMealDao.insertInstructions(instructions)
    }

    override suspend fun insertFavoriteMeal(favoriteMeal: FavoriteMeal) {
        favoriteMealDao.insertFavoriteMeal(favoriteMeal) // Save locally

        try {
            // Correctly store the favorite meal as a document inside the 'favorite_meals' collection
            userMealsCollection().document(favoriteMeal.id).set(favoriteMeal).await()
        } catch (e: Exception) {
            // Handle any errors during the Firestore operation
            e.printStackTrace()
        }
    }

    override suspend fun updateMeal(meal: Meal) {
        favoriteMealDao.updateMeal(meal)

        CoroutineScope(Dispatchers.IO).launch {
            userMealsCollection().document(meal.id).set(meal) // ✅ Update under user
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
            userMealsCollection().document(favoriteMeal.id).set(favoriteMeal) // ✅ Update under user
        }
    }

    override suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.insertMealWithDetails(mealWithDetails)

        CoroutineScope(Dispatchers.IO).launch {
            userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails)
        }
    }

    override suspend fun updateMealWithDetails(mealWithDetails: MealWithDetails) {
        favoriteMealDao.updateMealWithDetails(mealWithDetails)

        CoroutineScope(Dispatchers.IO).launch {
            userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails)
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
            userMealsCollection().document(mealId).delete() // ✅ Delete only for the user
        }
    }

    override suspend fun updateMealAsNotFavorite(mealId: String) {
        favoriteMealDao.updateMealAsNotFavorite(mealId)
    }

    override suspend fun removeFromFavorites(mealId: String) {
        favoriteMealDao.removeFromFavorites(mealId)

        CoroutineScope(Dispatchers.IO).launch {
            userMealsCollection().document(mealId).delete()
        }
    }

    suspend fun syncFromFirebase() {
        try {
            // ✅ Fetch only the authenticated user's favorite meals
            val favoriteMealSnapshot = userMealsCollection().get().await()
            val favoriteMeals: List<FavoriteMeal> = favoriteMealSnapshot.documents.mapNotNull { doc ->
                Log.d("SyncingFavoriteMealDao", "Syncing favorite meal with ID: ${doc.id}")
                FavoriteMeal(id = doc.get("id").toString())
            }

            // ✅ Insert favorite meals into local database
            favoriteMeals.forEachIndexed { index, favoriteMeal ->
                // Log the current meal ID for debugging
                Log.d("SyncingFavoriteMealDao", "Inserting favorite meal with ID: ${favoriteMeal.id}")

                // Check if it's not the last item in the list
                if (index != favoriteMeals.size - 1) {
                    // Insert only if the ID is valid (non-null, non-empty, and exactly 5 characters long)
                    if (!favoriteMeal.id.isNullOrEmpty() && favoriteMeal.id.length == 5) {
                        favoriteMealDao.insertFavoriteMeal(favoriteMeal)
                    } else {
                        Log.d("SyncingFavoriteMealDao", "Skipping meal with invalid ID: ${favoriteMeal.id}")
                    }
                } else {
                    // Log that the last item is being skipped
                    Log.d("SyncingFavoriteMealDao", "Skipping insertion for the last meal with ID: ${favoriteMeal.id}")
                }
            }



        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
