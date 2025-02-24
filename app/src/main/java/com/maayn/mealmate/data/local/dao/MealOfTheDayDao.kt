package com.maayn.mealmate.data.local.dao

import androidx.room.*
import com.maayn.mealmate.data.local.entities.MealOfTheDay
import com.maayn.mealmate.data.local.entities.MealWithDetails

@Dao
interface MealOfTheDayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMealOfTheDay(meal: MealOfTheDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealsOfTheDay(meals: List<MealOfTheDay>)

    @Transaction
    @Query("SELECT * FROM meals WHERE id = (SELECT mealId FROM meal_of_the_day WHERE date = :today LIMIT 1)")
    suspend fun getMealOfTheDayDetails(today: String): MealWithDetails?
}
