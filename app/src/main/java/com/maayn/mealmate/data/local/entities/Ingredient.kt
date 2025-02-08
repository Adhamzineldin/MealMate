package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey val id: String,
    val name: String,
    val quantity: String  // e.g., "2 cups", "1 tbsp"
)
