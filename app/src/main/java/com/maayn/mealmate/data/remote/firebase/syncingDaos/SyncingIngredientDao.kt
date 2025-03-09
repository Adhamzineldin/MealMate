package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.IngredientDao
import com.maayn.mealmate.data.local.entities.Ingredient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingIngredientDao(
    private val ingredientDao: IngredientDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid // Default to current user if null

) : IngredientDao {

    private fun userIngredientsCollection() =
        firestore.collection("users").document(userId.toString()).collection("ingredients")

    override suspend fun insertIngredient(ingredient: Ingredient) {
        ingredientDao.insertIngredient(ingredient) // Save locally

        CoroutineScope(Dispatchers.IO).launch {
            userIngredientsCollection().document(ingredient.id).set(ingredient) // ✅ Store under user
        }
    }

    override suspend fun insertIngredients(ingredients: List<Ingredient>) {
        ingredientDao.insertIngredients(ingredients) // Save locally

        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            ingredients.forEach { ingredient ->
                val docRef = userIngredientsCollection().document(ingredient.id)
                batch.set(docRef, ingredient)
            }
            batch.commit() // ✅ Batch insert per user
        }
    }

    override suspend fun getAllIngredients(): List<Ingredient> {
        return ingredientDao.getAllIngredients() // Fetch locally
    }

    suspend fun syncFromFirebase() {
        try {
            // ✅ Fetch only ingredients for the authenticated user
            val snapshot = userIngredientsCollection().get().await()
            val ingredients: List<Ingredient> = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ingredient::class.java)
            }

            if (ingredients.isNotEmpty()) {
                ingredientDao.insertIngredients(ingredients) // ✅ Insert locally
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
