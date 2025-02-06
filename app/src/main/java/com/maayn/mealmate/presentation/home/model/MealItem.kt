package com.maayn.mealmate.presentation.home.model

data class MealItem(
    val name: String,          // Name of the menu item
    val extraCount: Int = 0,   // Extra count for added ingredients
    val image: String = "",    // URL or resource name for the menu item image
    val id: Int = 0            // Unique identifier for the menu item
)
