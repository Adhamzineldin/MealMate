package com.maayn.mealmate.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    indices = [androidx.room.Index(value = ["name"], unique = true)] // Ensures the name is unique
)
data class ShoppingItem(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    var isChecked: Boolean = false
)
