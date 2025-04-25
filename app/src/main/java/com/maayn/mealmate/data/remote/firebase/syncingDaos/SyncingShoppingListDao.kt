package com.maayn.mealmate.data.remote.firebase.syncingDaos

import com.google.firebase.auth.FirebaseAuth
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
    private val firestore: FirebaseFirestore,
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid // Default to current user if null

) : ShoppingListDao {

    private fun userShoppingListCollection() =
        firestore.collection("users").document(userId.toString()).collection("shopping_list")

    // 🔹 **INSERT ITEM**
    override suspend fun insertItem(item: ShoppingList) {
        shoppingListDao.insertItem(item) // ✅ Save locally

        CoroutineScope(Dispatchers.IO).launch {
            userShoppingListCollection().document(item.id.toString()).set(item)
        }
    }

    // 🔹 **INSERT SHOPPING ITEMS**
    override suspend fun insertShoppingItems(shoppingItems: List<Ingredient>) {
        shoppingListDao.insertShoppingItems(shoppingItems) // ✅ Save locally

        CoroutineScope(Dispatchers.IO).launch {
            val batch = firestore.batch()
            shoppingItems.forEach { ingredient ->
                val docRef = userShoppingListCollection().document(ingredient.id)
                batch.set(docRef, ingredient)
            }
            batch.commit()
        }
    }

    // 🔹 **GET SHOPPING LIST**
    override suspend fun getShoppingList(): List<ShoppingList> {
        val snapshot = userShoppingListCollection().get().await()
        val items = snapshot.toObjects(ShoppingList::class.java)

        // Store items locally
        items.forEach { shoppingListDao.insertItem(it) }

        return shoppingListDao.getShoppingList()
    }
}
