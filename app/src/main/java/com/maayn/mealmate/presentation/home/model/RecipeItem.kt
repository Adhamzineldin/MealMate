package com.maayn.mealmate.presentation.home.model

import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails
import kotlin.random.Random

data class RecipeItem(
    val id: String,
    val name: String,
    val category: String = "Unknown",
    val area: String = "Unknown",
    val instructions: List<Instruction> = emptyList(),
    val imageUrl: String,
    val youtubeUrl: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    var isFavorited: Boolean = false,
    val rating: Float = Random.nextDouble(3.0, 5.0).toFloat(),
    val ratingCount: Int = Random.nextInt(10, 1000),
    val time: String = "${Random.nextInt(10, 90)} min"
) {
    data class Ingredient(val name: String, val measure: String)
    data class Instruction(val step: String)
}

fun RecipeItem.toMealWithDetails(): MealWithDetails {
    val meal = Meal(
        id = this.id,
        name = this.name,
        category = this.category,
        country = this.area,
        imageUrl = this.imageUrl,
        videoUrl = this.youtubeUrl ?: "", // Ensure null safety
        isFavorite = this.isFavorited
    )

    val ingredientEntities = ingredients.map {
        IngredientEntity(
            name = it.name,
            measure = it.measure,
            mealId = this.id
        )
    }

    val instructionEntities = instructions.mapIndexed { index, instruction ->
        InstructionEntity(
            mealId = this.id,
            step = (index + 1).toString(),  // Assign step numbers correctly
            description = instruction.step
        )
    }

    return MealWithDetails(
        meal = meal,
        ingredients = ingredientEntities,
        instructions = instructionEntities
    )
}
