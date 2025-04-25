package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.ShoppingItemDao
import com.maayn.mealmate.data.local.entities.ShoppingItem
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class SyncingShoppingItemDao(
    private val shoppingItemDao: ShoppingItemDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
) : ShoppingItemDao {

    private fun userShoppingItemsCollection() =
        firestore.collection("users").document(userId ?: "").collection("shopping_items")

    override suspend fun insert(item: ShoppingItem) {
        // Save locally first
        shoppingItemDao.insert(item)

        // Then sync to Firebase
        withContext(Dispatchers.IO) {
            try {
                userShoppingItemsCollection().document(item.id).set(item).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun delete(item: ShoppingItem) {
        // Delete locally first
        shoppingItemDao.delete(item)

        // Then delete from Firebase
        withContext(Dispatchers.IO) {
            try {
                userShoppingItemsCollection().document(item.id).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getAll(): List<ShoppingItem> {
        try {
            // Get from Firebase first to ensure we have latest data
            val snapshot = userShoppingItemsCollection().get().await()
            val items = snapshot.toObjects(ShoppingItem::class.java)

            // Store items locally if we have any
            if (items.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    items.forEach { shoppingItemDao.insert(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On Firebase failure, fall back to local data
        }

        // Return local data
        return shoppingItemDao.getAll()
    }

    override suspend fun deleteAll() {
        // Delete locally first
        shoppingItemDao.deleteAll()

        // Then delete from Firebase
        withContext(Dispatchers.IO) {
            try {
                val batch = firestore.batch()
                val documents = userShoppingItemsCollection().get().await().documents

                documents.forEach {
                    batch.delete(it.reference)
                }

                if (documents.isNotEmpty()) {
                    batch.commit().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun update(item: ShoppingItem) {
        // Update locally first
        shoppingItemDao.update(item)

        // Then update in Firebase
        withContext(Dispatchers.IO) {
            try {
                userShoppingItemsCollection().document(item.id).set(item).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncFromFirebase() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = userShoppingItemsCollection().get().await()
                val items = snapshot.documents.mapNotNull {
                    it.toObject(ShoppingItem::class.java)
                }

                if (items.isNotEmpty()) {
                    items.forEach { shoppingItemDao.insert(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
