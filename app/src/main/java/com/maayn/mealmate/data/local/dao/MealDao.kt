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
    suspend fun insertMeals(meals: List<Meal>)  // ✅ Batch insert

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

    @Transaction
    suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        insertMealWithDetails(mealWithDetails.meal, mealWithDetails.ingredients, mealWithDetails.instructions)
    }

    @Transaction
    suspend fun insertMealsWithDetails(meals: List<MealWithDetails>) {
        for (mealWithDetails in meals) {
            insertMealWithDetails(mealWithDetails.meal, mealWithDetails.ingredients, mealWithDetails.instructions)
        }
    }

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithDetails(mealId: String): MealWithDetails

    @Transaction
    @Query("SELECT * FROM meals")
    suspend fun getAllMealWithDetails(): List<MealWithDetails>

    @Transaction
    @Query("SELECT * FROM meals WHERE category = :category")
    suspend fun getMealsWithDetailsByCategory(category: String): List<MealWithDetails>  // ✅ Added function

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

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getMealById(mealId: String): Meal?
}
