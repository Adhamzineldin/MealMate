package com.maayn.mealmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.maayn.mealmate.data.local.entities.MealPlan

@Dao
interface MealPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan)

    @Query("SELECT * FROM meal_plans ORDER BY id DESC")
    fun getAllMealPlans(): LiveData<List<MealPlan>>

    @Query("SELECT * FROM meal_plans WHERE date >= :today ORDER BY date ASC")
    fun getUpcomingMealPlans(today: String): LiveData<List<MealPlan>>

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: Int?): MealPlan?

    @Query("SELECT * FROM meal_plans WHERE firebaseId = :firebaseId")
    suspend fun getMealPlanByFirebaseId(firebaseId: String?): List<MealPlan>?

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)
}

