package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ingredientName: String,
    val quantity: String,
    val isChecked: Boolean = false  // Track if the user bought it
)
