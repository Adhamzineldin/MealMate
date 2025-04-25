package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.ShoppingListDao
import com.maayn.mealmate.data.local.entities.Ingredient
import com.maayn.mealmate.data.local.entities.ShoppingList
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class SyncingShoppingListDao(
    private val shoppingListDao: ShoppingListDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : ShoppingListDao {

    private fun userShoppingListCollection() =
        firestore.collection("users").document(userId ?: "").collection("shopping_list")

    override suspend fun insertItem(item: ShoppingList) {
        // Save locally first
        shoppingListDao.insertItem(item)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userShoppingListCollection().document(item.id.toString()).set(item).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertShoppingItems(shoppingItems: List<Ingredient>) {
        // Save locally first
        shoppingListDao.insertShoppingItems(shoppingItems)

        // Then batch upload to Firebase
        withContext(Dispatchers.IO) {
            try {
                if (shoppingItems.isNotEmpty()) {
                    val batch = firestore.batch()
                    shoppingItems.forEach { ingredient ->
                        val docRef = userShoppingListCollection().document(ingredient.id)
                        batch.set(docRef, ingredient)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getShoppingList(): List<ShoppingList> {
        try {
            // Get from Firebase first to ensure we have latest data
            val snapshot = userShoppingListCollection().get().await()
            val items = snapshot.toObjects(ShoppingList::class.java)

            // Store items locally if we have any
            if (items.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    items.forEach { shoppingListDao.insertItem(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On Firebase failure, fall back to local data
        }

        // Return local data
        return shoppingListDao.getShoppingList()
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = userShoppingListCollection().get().await()
                val items = snapshot.documents.mapNotNull {
                    it.toObject(ShoppingList::class.java)
                }

                if (items.isNotEmpty()) {
                    items.forEach { shoppingListDao.insertItem(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}