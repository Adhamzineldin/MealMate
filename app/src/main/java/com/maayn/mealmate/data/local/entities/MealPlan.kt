package com.maayn.mealmate.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,       // Meal plan name (e.g., "Weekly Healthy Meals")
    val date: String,       // Date in format "DD/MM/YYYY"
    val mealType: String,   // Meal type (e.g., "Breakfast", "Lunch", "Dinner")
    val recipeName: String, // Recipe name
    val recipeImage: String // Image URL or resource
)
