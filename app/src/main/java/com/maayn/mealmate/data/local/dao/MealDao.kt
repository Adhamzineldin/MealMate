package com.maayn.mealmate.data.local.dao

import androidx.room.*
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.MealWithDetails

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<InstructionEntity>)

    @Transaction
    suspend fun insertMealWithDetails(meal: Meal, ingredients: List<IngredientEntity>, instructions: List<InstructionEntity>) {
        insertMeal(meal)
        insertIngredients(ingredients)
        insertInstructions(instructions)
    }

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithDetails(mealId: String): MealWithDetails?

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMeal(mealId: String)

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteMealIngredients(mealId: String)

    @Query("DELETE FROM meal_instructions WHERE mealId = :mealId")
    suspend fun deleteMealInstructions(mealId: String)

    @Transaction
    suspend fun deleteMealWithDetails(mealId: String) {
        deleteMealIngredients(mealId)
        deleteMealInstructions(mealId)
        deleteMeal(mealId)
    }
}
