package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.ShoppingItemDao
import com.maayn.mealmate.data.local.entities.ShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingShoppingItemDao(
    private val shoppingItemDao: ShoppingItemDao,
    private val firestore: FirebaseFirestore
) : ShoppingItemDao {

    override suspend fun insert(item: ShoppingItem) {
        shoppingItemDao.insert(item) // Save locally

        // Sync to Firebase in the background
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("shopping_items").document(item.id).set(item)
        }
    }

    override suspend fun delete(item: ShoppingItem) {
        shoppingItemDao.delete(item) // Delete locally

        // Remove from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("shopping_items").document(item.id).delete()
        }
    }

    override suspend fun getAll(): List<ShoppingItem> {
        // Fetch from Firestore first
        val snapshot = firestore.collection("shopping_items").get().await()
        val items = snapshot.toObjects(ShoppingItem::class.java)

        // Store items locally
        items.forEach { shoppingItemDao.insert(it) }

        // Return locally stored items
        return shoppingItemDao.getAll()
    }

    override suspend fun deleteAll() {
        shoppingItemDao.deleteAll() // Delete locally

        // Delete all from Firestore
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("shopping_items").get().await().documents.forEach {
                it.reference.delete()
            }
        }
    }

    override suspend fun update(item: ShoppingItem) {
        shoppingItemDao.update(item) // Update locally

        // Sync updated item to Firebase
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("shopping_items").document(item.id).set(item)
        }
    }
}
