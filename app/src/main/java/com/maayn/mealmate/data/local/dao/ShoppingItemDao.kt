package com.maayn.mealmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.maayn.mealmate.data.local.entities.ShoppingItem

@Dao
interface ShoppingItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignores insertion if name already exists
    suspend fun insert(item: ShoppingItem)

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("SELECT * FROM shopping_items")
    suspend fun getAll(): List<ShoppingItem>

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    @Update
    suspend fun update(item: ShoppingItem)
}
