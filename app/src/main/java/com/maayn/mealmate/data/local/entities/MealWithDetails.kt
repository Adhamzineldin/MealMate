package com.maayn.mealmate.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.maayn.mealmate.presentation.home.model.RecipeItem

data class MealWithDetails(
    @Embedded val meal: Meal = Meal(), // Ensure default value for Firestore

    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val ingredients: List<IngredientEntity> = emptyList(), // Avoid nullability issues

    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val instructions: List<InstructionEntity> = emptyList() // Avoid nullability issues
) {
    fun toRecipeItem(): RecipeItem {
        return RecipeItem(
            id = meal.id,
            name = meal.name,
            category = meal.category,
            area = meal.country,
            imageUrl = meal.imageUrl,
            youtubeUrl = meal.videoUrl,
            isFavorited = meal.isFavorite,
            ingredients = ingredients.map { it.toDomain() },
            instructions = instructions.map { it.toDomain() },
            time = meal.time,
            rating = meal.rating,
            ratingCount = meal.ratingCount
        )
    }
}

// Helper functions for mapping entities to domain models
fun IngredientEntity.toDomain() = RecipeItem.Ingredient(name = this.name, measure = this.measure)

fun InstructionEntity.toDomain() = RecipeItem.Instruction(step = this.step) // Fix missing description mapping
