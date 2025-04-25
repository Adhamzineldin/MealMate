package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.IngredientDao
import com.maayn.mealmate.data.local.entities.Ingredient
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class SyncingIngredientDao(
    private val ingredientDao: IngredientDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : IngredientDao {

    private fun userIngredientsCollection() =
        firestore.collection("users").document(userId ?: "").collection("ingredients")

    override suspend fun insertIngredient(ingredient: Ingredient) {
        // Save locally first
        ingredientDao.insertIngredient(ingredient)

        // Then sync to Firebase on the IO dispatcher without creating a new scope
        withContext(Dispatchers.IO) {
            try {
                userIngredientsCollection().document(ingredient.id).set(ingredient).await()
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertIngredients(ingredients: List<Ingredient>) {
        // Save locally first
        ingredientDao.insertIngredients(ingredients)

        // Then batch upload to Firebase
        withContext(Dispatchers.IO) {
            try {
                // Only proceed if there are ingredients to upload
                if (ingredients.isNotEmpty()) {
                    val batch = firestore.batch()
                    ingredients.forEach { ingredient ->
                        val docRef = userIngredientsCollection().document(ingredient.id)
                        batch.set(docRef, ingredient)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients()
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = userIngredientsCollection().get().await()
                val ingredients = snapshot.documents.mapNotNull { it.toObject(Ingredient::class.java) }

                // Only insert if we have data to avoid unnecessary operations
                if (ingredients.isNotEmpty()) {
                    ingredientDao.insertIngredients(ingredients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}