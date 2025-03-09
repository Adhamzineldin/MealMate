package com.maayn.mealmate.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    indices = [Index(value = ["name"], unique = true)] // Ensures the name is unique
)
data class ShoppingItem(
    @PrimaryKey val id: String = "", // Default value for Firestore compatibility
    @ColumnInfo(name = "name") val name: String = "", // Default value for safety
    var isChecked: Boolean = false
) {
    // Explicit no-arg constructor for Firestore
    constructor() : this("", "", false)
}
