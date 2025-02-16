package com.maayn.mealmate.presentation.home.model

data class RecipeItem(
    val id: String,
    val name: String,
    val time: String,
    val rating: Float,
    val imageUrl: String,
    val ratingCount: Int = 0,
    val category: String
)