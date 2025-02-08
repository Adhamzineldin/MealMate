package com.maayn.mealmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maayn.mealmate.data.local.entities.Ingredient
import com.maayn.mealmate.data.local.entities.ShoppingList

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(shoppingItems: List<Ingredient>)

    @Query("SELECT * FROM shopping_list")
    suspend fun getShoppingList(): List<ShoppingList>
}
