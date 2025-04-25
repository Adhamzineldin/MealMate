package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.maayn.mealmate.data.local.dao.ShoppingItemDao
import com.maayn.mealmate.data.local.entities.ShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncingShoppingItemDao(
    private val shoppingItemDao: ShoppingItemDao,
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid // Default to current user if null

) : ShoppingItemDao {

    private fun userShoppingItemsCollection() =
        firestore.collection("users").document(userId.toString()).collection("shopping_items")

    // 🔹 **INSERT ITEM**
    override suspend fun insert(item: ShoppingItem) {
        shoppingItemDao.insert(item) // ✅ Save locally

        CoroutineScope(Dispatchers.IO).launch {
            userShoppingItemsCollection().document(item.id).set(item)
        }
    }

    // 🔹 **DELETE ITEM**
    override suspend fun delete(item: ShoppingItem) {
        shoppingItemDao.delete(item) // ✅ Delete locally

        CoroutineScope(Dispatchers.IO).launch {
            userShoppingItemsCollection().document(item.id).delete()
        }
    }

    // 🔹 **GET ALL ITEMS**
    override suspend fun getAll(): List<ShoppingItem> {
        val snapshot = userShoppingItemsCollection().get().await()
        val items = snapshot.toObjects(ShoppingItem::class.java)

        // Store items locally
        items.forEach { shoppingItemDao.insert(it) }

        return shoppingItemDao.getAll()
    }

    // 🔹 **DELETE ALL ITEMS**
    override suspend fun deleteAll() {
        shoppingItemDao.deleteAll() // ✅ Delete locally

        CoroutineScope(Dispatchers.IO).launch {
            userShoppingItemsCollection().get().await().documents.forEach {
                it.reference.delete()
            }
        }
    }

    // 🔹 **UPDATE ITEM**
    override suspend fun update(item: ShoppingItem) {
        shoppingItemDao.update(item) // ✅ Update locally

        CoroutineScope(Dispatchers.IO).launch {
            userShoppingItemsCollection().document(item.id).set(item)
        }
    }

    // 🔹 **SYNC ITEMS FROM FIREBASE**
    suspend fun syncFromFirebase() {
        try {
            val snapshot = userShoppingItemsCollection().get().await()
            val items: List<ShoppingItem> = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ShoppingItem::class.java)
            }

            if (items.isNotEmpty()) {
                items.forEach { shoppingItemDao.insert(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
