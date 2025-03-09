package com.maayn.mealmate.data.local.entities

import java.io.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,  // Room's auto-generated ID
    var firebaseId: String? = null,  // Firestore document ID (null until Firestore assigns it)
    var name: String = "",
    var date: String? = "",
    var mealType: String = "",
    var recipeName: String = "",
    var recipeImage: String = "",
    var recipeId: String = ""
) : Serializable {
    constructor() : this(null, null, "", "", "", "", "", "")
}

