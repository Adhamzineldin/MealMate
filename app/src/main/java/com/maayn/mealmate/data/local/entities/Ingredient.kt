package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var quantity: String = ""  // e.g., "2 cups", "1 tbsp"
) {
    constructor() : this("", "", "") // No-argument constructor for Firestore
}
