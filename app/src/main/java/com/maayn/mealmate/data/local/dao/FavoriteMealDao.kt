package com.maayn.mealmate.data.local.dao

import androidx.room.*
import com.maayn.mealmate.data.local.entities.FavoriteMeal
import com.maayn.mealmate.data.local.entities.IngredientEntity
import com.maayn.mealmate.data.local.entities.InstructionEntity
import com.maayn.mealmate.data.local.entities.Meal
import com.maayn.mealmate.data.local.entities.MealWithDetails

@Dao
interface FavoriteMealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<InstructionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteMeal(favoriteMeal: FavoriteMeal)

    @Update
    suspend fun updateMeal(meal: Meal)

    @Update
    suspend fun updateIngredients(ingredients: List<IngredientEntity>)

    @Update
    suspend fun updateInstructions(instructions: List<InstructionEntity>)

    @Update
    suspend fun updateFavoriteMeal(favoriteMeal: FavoriteMeal)

    @Transaction
    suspend fun insertMealWithDetails(mealWithDetails: MealWithDetails) {
        insertMeal(mealWithDetails.meal)
        insertIngredients(mealWithDetails.ingredients)
        insertInstructions(mealWithDetails.instructions)

        // Convert MealWithDetails → FavoriteMeal and insert
        val favoriteMeal = FavoriteMeal(id = mealWithDetails.meal.id,)
        insertFavoriteMeal(favoriteMeal)
    }

    @Transaction
    suspend fun updateMealWithDetails(mealWithDetails: MealWithDetails) {
        updateMeal(mealWithDetails.meal)
        updateIngredients(mealWithDetails.ingredients)
        updateInstructions(mealWithDetails.instructions)

        // Convert MealWithDetails → FavoriteMeal and update
        val favoriteMeal = FavoriteMeal(id = mealWithDetails.meal.id)
        updateFavoriteMeal(favoriteMeal)
    }

    @Transaction
    @Query("SELECT * FROM meals WHERE id IN (SELECT id FROM favorite_meals)")
    suspend fun getAllFavoriteMealDetails(): List<MealWithDetails>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId AND id IN (SELECT id FROM favorite_meals)")
    suspend fun getFavoriteMealDetailsById(mealId: String): MealWithDetails?

    @Query("DELETE FROM favorite_meals WHERE id = :mealId")
    suspend fun deleteFavoriteMeal(mealId: String)

    @Query("UPDATE meals SET isFavorite = 0 WHERE id = :mealId")
    suspend fun updateMealAsNotFavorite(mealId: String)

    @Transaction
    suspend fun removeFromFavorites(mealId: String) {
        deleteFavoriteMeal(mealId)
        updateMealAsNotFavorite(mealId)
    }


}

