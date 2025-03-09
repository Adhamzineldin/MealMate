package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Keep auto-generation for Room
    val ingredientName: String = "", // Default value for Firestore compatibility
    val quantity: String = "", // Default value to prevent null issues
    val isChecked: Boolean = false // Track if the user bought it
) {
    // Explicit no-arg constructor for Firestore deserialization
    constructor() : this(0, "", "", false)
}
