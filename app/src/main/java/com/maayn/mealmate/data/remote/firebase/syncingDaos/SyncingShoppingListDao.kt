package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.ShoppingListDao
import com.maayn.mealmate.data.local.entities.Ingredient
import com.maayn.mealmate.data.local.entities.ShoppingList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingShoppingListDao(
    private val shoppingListDao: ShoppingListDao,
    private val firestore: FirebaseFirestore
) : ShoppingListDao {

    override suspend fun insertItem(item: ShoppingList) {
        shoppingListDao.insertItem(item) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("shopping_list").document(item.id.toString()).set(item)
        }
    }

    override suspend fun insertShoppingItems(shoppingItems: List<Ingredient>) {
        shoppingListDao.insertShoppingItems(shoppingItems) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            shoppingItems.forEach { ingredient ->
                val docRef = firestore.collection("shopping_list").document(ingredient.id)
                batch.set(docRef, ingredient)
            }
            batch.commit()
        }
    }

    override suspend fun getShoppingList(): List<ShoppingList> {
        // Fetch from Firestore first
        val snapshot = firestore.collection("shopping_list").get().await()
        val items = snapshot.toObjects(ShoppingList::class.java)

        // Store items locally
        items.forEach { shoppingListDao.insertItem(it) }

        // Return locally stored items
        return shoppingListDao.getShoppingList()
    }
}
