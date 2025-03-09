package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_of_the_day")
data class MealOfTheDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealId: String, // Links to a Meal
    val date: String = "" // Ensures default value for Firestore compatibility
) {
    // No-arg constructor for Firebase deserialization
    constructor() : this(0, "")
}
