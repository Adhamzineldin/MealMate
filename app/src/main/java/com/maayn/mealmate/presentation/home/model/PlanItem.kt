package com.maayn.mealmate.presentation.home.model


data class PlanItem(
    val id: String,               // Unique identifier for the plan item
    val mealType: MealType,        // Breakfast, Lunch, Dinner
    val date: String,              // Date of the plan
    val duration: Int,             // Duration in minutes
    val servings: Int,             // Number of servings
    val menuItems: List<MealItem>, // List of MenuItems (for each plan)
    val isCooked: Boolean = false, // Whether the plan is cooked
    val isFavorite: Boolean = false, // Whether the plan is marked as favorite
    val dynamicImageUrl: String? = null, // Optional dynamic image URL for the meal (if available)
    val image: String? = null // Optional static image URL or resource ID (if any)
)
