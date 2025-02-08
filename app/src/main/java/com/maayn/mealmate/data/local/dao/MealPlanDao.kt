package com.maayn.mealmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.maayn.mealmate.data.local.entities.MealPlan

@Dao
interface MealPlanDao {
    @Insert
    suspend fun addMealToPlan(mealPlan: MealPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlan>)

    @Query("SELECT * FROM meal_plans WHERE userId = :userId")
    suspend fun getMealPlansForUser(userId: String): List<MealPlan>

    @Query("SELECT * FROM meal_plans WHERE userId = :userId AND dayOfWeek = :dayOfWeek")
    suspend fun getMealPlanForDay(userId: String, dayOfWeek: String): MealPlan?
}
