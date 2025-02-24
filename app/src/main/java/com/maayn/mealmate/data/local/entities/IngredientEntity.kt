package com.maayn.mealmate.data.local.entities

import androidx.room.Entity

@Entity(tableName = "meal_ingredients", primaryKeys = ["mealId", "name"])
data class IngredientEntity(
    val mealId: String,  // Foreign key to Meal
    val name: String,
    val measure: String
)
