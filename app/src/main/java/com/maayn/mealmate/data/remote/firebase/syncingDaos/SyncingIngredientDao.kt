package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.IngredientDao
import com.maayn.mealmate.data.local.entities.Ingredient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingIngredientDao(
    private val ingredientDao: IngredientDao,
    private val firestore: FirebaseFirestore
) : IngredientDao {

    override suspend fun insertIngredient(ingredient: Ingredient) {
        ingredientDao.insertIngredient(ingredient) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("ingredients").document(ingredient.id).set(ingredient)
        }
    }

    override suspend fun insertIngredients(ingredients: List<Ingredient>) {
        ingredientDao.insertIngredients(ingredients) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            ingredients.forEach { ingredient ->
                val docRef = firestore.collection("ingredients").document(ingredient.id)
                batch.set(docRef, ingredient)
            }
            batch.commit()
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients() // Fetch locally
    }

    suspend fun syncFromFirebase() {
        try {
            val snapshot = firestore.collection("ingredients").get().await()
            val ingredients: List<Ingredient> = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ingredient::class.java)
            }

            // Insert only if new data exists
            if (ingredients.isNotEmpty()) {
                ingredientDao.insertIngredients(ingredients) // Insert new data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
