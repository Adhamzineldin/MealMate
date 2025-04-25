package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.FavoriteMealDao
import com.maayn.mealmate.data.local.entities.FavoriteMeal
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class SyncingFavoriteMealDao(
    private val favoriteMealDao: FavoriteMealDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : FavoriteMealDao {

    private fun userMealsCollection() =
        firestore.collection("users").document(userId ?: "").collection("favorite_meals")

    override suspend fun insertMeal(meal: Meal) {
        // Save locally first
        favoriteMealDao.insertMeal(meal)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(meal.id).set(meal).await()
            } catch (e: Exception) {
                e.printStackTrace()
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
        // Save locally first
        favoriteMealDao.insertFavoriteMeal(favoriteMeal)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(favoriteMeal.id).set(favoriteMeal).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updateMeal(meal: Meal) {
        // Update locally first
        favoriteMealDao.updateMeal(meal)

        // Then update in Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(meal.id).set(meal).await()
            } catch (e: Exception) {
                e.printStackTrace()
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
        // Update locally first
        favoriteMealDao.updateFavoriteMeal(favoriteMeal)

        // Then update in Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(favoriteMeal.id).set(favoriteMeal).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        // Insert locally first
        favoriteMealDao.insertMealWithDetails(mealWithDetails)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updateMealWithDetails(mealWithDetails: MealWithDetails) {
        // Update locally first
        favoriteMealDao.updateMealWithDetails(mealWithDetails)

        // Then update in Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(mealWithDetails.meal.id).set(mealWithDetails).await()
            } catch (e: Exception) {
                e.printStackTrace()
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
        // Delete locally first
        favoriteMealDao.deleteFavoriteMeal(mealId)

        // Then delete from Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(mealId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updateMealAsNotFavorite(mealId: String) {
        favoriteMealDao.updateMealAsNotFavorite(mealId)
    }

    override suspend fun removeFromFavorites(mealId: String) {
        // Remove locally first
        favoriteMealDao.removeFromFavorites(mealId)

        // Then remove from Firebase
        withContext(Dispatchers.IO) {
            try {
                userMealsCollection().document(mealId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val favoriteMealSnapshot = userMealsCollection().get().await()
                val favoriteMeals = favoriteMealSnapshot.documents.mapNotNull {
                    // Convert to FavoriteMeal object
                    val meal = it.toObject(FavoriteMeal::class.java)

                    // Only return valid meals (with at least an ID)
                    if (meal != null && meal.id.isNotEmpty()) meal else null
                }

                if (favoriteMeals.isNotEmpty()) {
                    favoriteMeals.forEach { favoriteMeal ->
                        favoriteMealDao.insertFavoriteMeal(favoriteMeal)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}