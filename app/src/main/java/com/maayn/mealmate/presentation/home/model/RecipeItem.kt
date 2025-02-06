package com.maayn.mealmate.presentation.home.model

data class RecipeItem(
    val idMeal: String,
    val name: String,
    val duration: String,
    val rating: Float,
    val image: String,
    val ratingCount: Int = 0
)