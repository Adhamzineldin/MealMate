package com.maayn.mealmate.data.model

data class RecipeResponse(val meals: List<Recipe>)
data class Recipe(val idMeal: String, val strMeal: String, val strMealThumb: String)
