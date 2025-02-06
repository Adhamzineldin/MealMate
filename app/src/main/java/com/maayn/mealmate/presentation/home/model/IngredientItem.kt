package com.maayn.mealmate.presentation.home.model

data class IngredientItem(
    val name: String,
    val imageUrl: String,
    val grams: Int,
    val isInCart: Boolean = false
)
