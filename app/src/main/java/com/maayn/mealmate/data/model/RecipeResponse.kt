package com.maayn.mealmate.data.model

import com.google.gson.annotations.SerializedName
import com.maayn.mealmate.presentation.home.model.RecipeItem

data class RecipeResponse(
    @SerializedName("meals") val meals: List<ApiMeal>?
)

data class ApiMeal(
    @SerializedName("idMeal") val id: String,
    @SerializedName("strMeal") val name: String,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?,
    @SerializedName("strInstructions") val instructions: String?,
    @SerializedName("strMealThumb") val imageUrl: String,
    @SerializedName("strYoutube") val youtubeUrl: String?,
    // Ingredients (up to 20, since API uses numbered fields)
    @SerializedName("strIngredient1") val ingredient1: String?,
    @SerializedName("strIngredient2") val ingredient2: String?,
    @SerializedName("strIngredient3") val ingredient3: String?,
    @SerializedName("strIngredient4") val ingredient4: String?,
    @SerializedName("strIngredient5") val ingredient5: String?,
    // Add more if needed...
    @SerializedName("strMeasure1") val measure1: String?,
    @SerializedName("strMeasure2") val measure2: String?,
    @SerializedName("strMeasure3") val measure3: String?,
    @SerializedName("strMeasure4") val measure4: String?,
    @SerializedName("strMeasure5") val measure5: String?
)
fun ApiMeal.extractIngredients(): List<RecipeItem.Ingredient> {
    return listOfNotNull(
        ingredient1?.takeIf { it.isNotBlank() }?.let { RecipeItem.Ingredient(it, measure1 ?: "") },
        ingredient2?.takeIf { it.isNotBlank() }?.let { RecipeItem.Ingredient(it, measure2 ?: "") },
        ingredient3?.takeIf { it.isNotBlank() }?.let { RecipeItem.Ingredient(it, measure3 ?: "") },
        ingredient4?.takeIf { it.isNotBlank() }?.let { RecipeItem.Ingredient(it, measure4 ?: "") },
        ingredient5?.takeIf { it.isNotBlank() }?.let { RecipeItem.Ingredient(it, measure5 ?: "") }
    )
}

// Convert instructions into a list of steps
fun ApiMeal.extractInstructions(): List<RecipeItem.Instruction> {
    return instructions?.split("\n")?.filter { it.isNotBlank() }?.map { RecipeItem.Instruction(it) } ?: emptyList()
}


