package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.io.Serializable

@Entity(
    tableName = "meal_plans",
    indices = [Index(value = ["firebaseId"], unique = true)]  // Ensure uniqueness
)
data class MealPlan(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    var firebaseId: String? = null,  // Firestore document ID (must be unique)
    var name: String = "",
    var date: String? = "",
    var mealType: String = "",
    var recipeName: String = "",
    var recipeImage: String = "",
    var recipeId: String = ""
) : Serializable {
    constructor() : this(null, null, "", "", "", "", "", "")
}
