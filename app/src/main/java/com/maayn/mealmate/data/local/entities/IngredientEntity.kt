package com.maayn.mealmate.data.local.entities

import androidx.room.Entity

@Entity(tableName = "meal_ingredients", primaryKeys = ["mealId", "name"])
data class IngredientEntity(
    var mealId: String = "",  // Foreign key to Meal
    var name: String = "",
    var measure: String = ""
) {
    constructor() : this("", "", "") // No-argument constructor for Firestore
}
